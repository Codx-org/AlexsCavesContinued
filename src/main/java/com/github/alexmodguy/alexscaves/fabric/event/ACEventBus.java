package com.github.alexmodguy.alexscaves.fabric.event;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.EventPriority;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.SubscribeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The Fabric side of the game event bus, standing in for the one the other two loaders own.
 *
 * <p><b>Why a real bus rather than direct calls.</b> The obvious cheap answer on Fabric is to skip
 * the bus and have each Fabric API callback invoke the handler method it maps to. That does not fit
 * this mod, because the bus is load-bearing in <i>both</i> directions here. The mod receives ~60
 * loader hooks, and it also <b>publishes</b> eight of its own — Citadel's seven event classes and
 * {@code AnimationEvent} — which exist precisely so other mods can steer this one. A direct-call
 * dispatcher answers the first half and silently drops the second: a downstream mod's listener
 * would have nothing to attach to. Reproducing dispatch is ~100 lines and answers both, keeps
 * {@code EVENT_BUS.register(new CommonEvents())} and all 20-odd {@code post(...)} call sites
 * byte-identical across the three loaders, and preserves the two semantics the handlers actually
 * rely on: priority order and cancellation.
 *
 * <p>Listeners are keyed on the declared parameter type and dispatched up the class hierarchy, so a
 * handler on a base event still sees its subclasses — {@code LivingEvent} sees
 * {@code LivingDeathEvent} — as it does on Forge.
 *
 * <p>What is <i>not</i> reproduced: generic-type filtering (Forge's {@code GenericEvent}), which
 * nothing in this tree uses; and listener removal, which nothing here does either. Both would be
 * additions to this class if a call site ever needed them — never to the Stonecutter rule that
 * points at it.
 */
public final class ACEventBus {

    /** Listeners as registered, keyed on the exact declared parameter type. */
    private final Map<Class<?>, List<Listener>> declared = new ConcurrentHashMap<>();

    /**
     * Per-concrete-event-class dispatch lists: every declared listener whose type is assignable
     * from it, flattened and sorted once. Cleared wholesale whenever a listener is added, which
     * happens a few dozen times at startup and never afterwards.
     */
    private final Map<Class<?>, List<Listener>> dispatch = new ConcurrentHashMap<>();

    /**
     * Adds every {@link SubscribeEvent} method on {@code handler}. Instance and static methods both
     * count, matching Forge, which is what lets a handler class mix the two.
     */
    public void register(Object handler) {
        Class<?> type = handler instanceof Class<?> clazz ? clazz : handler.getClass();
        for (Method method : type.getMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])) {
                AlexsCaves.LOGGER.error("Ignoring @SubscribeEvent method " + type.getName() + "#" + method.getName()
                        + " — a handler takes exactly one event parameter");
                continue;
            }
            Object target = Modifier.isStatic(method.getModifiers()) ? null : handler;
            add(parameters[0], new Listener(annotation.priority(), annotation.receiveCanceled(), event -> {
                try {
                    method.invoke(target, event);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not reach " + method, e);
                } catch (InvocationTargetException e) {
                    rethrow(e.getCause());
                }
            }));
        }
    }

    /**
     * Adds a single listener. Forge derives the event type from the lambda's own signature, which
     * has no portable equivalent, so the type is named explicitly — the reason every call site that
     * uses this form already passes a class literal.
     */
    public <T extends Event> void addListener(Class<T> eventType, Consumer<T> listener) {
        addListener(EventPriority.NORMAL, eventType, listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void addListener(EventPriority priority, Class<T> eventType, Consumer<T> listener) {
        add(eventType, new Listener(priority, false, event -> listener.accept((T) event)));
    }

    /**
     * Dispatches {@code event} and answers whether a listener cancelled it — the return value bus 6
     * gives, which is what the shared source reads (the newer buses hand the event back instead,
     * and those call sites are gated).
     */
    public boolean post(Event event) {
        for (Listener listener : dispatch.computeIfAbsent(event.getClass(), this::resolve)) {
            if (!event.isCanceled() || listener.receiveCanceled) {
                listener.invoke.accept(event);
            }
        }
        return event.isCanceled();
    }

    private void add(Class<?> eventType, Listener listener) {
        declared.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(listener);
        dispatch.clear();
    }

    /**
     * Collects the listeners that apply to one concrete event class by walking its superclasses,
     * then orders them by priority. The sort is stable, so listeners at the same priority keep
     * registration order — again matching Forge, and the reason the two proxies' registration order
     * is worth leaving alone.
     */
    private List<Listener> resolve(Class<?> eventType) {
        List<Listener> applicable = new ArrayList<>();
        for (Class<?> type = eventType; type != null && Event.class.isAssignableFrom(type); type = type.getSuperclass()) {
            List<Listener> here = declared.get(type);
            if (here != null) {
                applicable.addAll(here);
            }
        }
        applicable.sort(Comparator.comparingInt(listener -> listener.priority.ordinal()));
        return applicable;
    }

    /**
     * Lets a handler's exception out unchanged rather than wrapped in the reflection layer's own —
     * a mod crash should read the same on this loader as on the other two.
     */
    private static void rethrow(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(cause);
    }

    private record Listener(EventPriority priority, boolean receiveCanceled, Consumer<Event> invoke) {
    }
}
