package org.teatrove.tea.compiler;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import org.teatrove.tea.annotations.Around;

public class Chain {

    // private int index;
    private Class<?> type;
    private Parsers parsers;
    private ParserDefinition definition;
    private ParserDefinition[] definitions;
    private Stack<ChainState> stack = new Stack<ChainState>();

    public Chain(Parsers parsers, ParserDefinition definition) {
        this.parsers = parsers;
        this.definition = definition;
    }

    public Chain(Parsers parsers, ParserDefinition[] definitions) {
        this.parsers = parsers;
        this.definitions = definitions;
    }

    public class ChainState {
        private Object instance;
        private Method invoked;
        private Object[] parameters;
        private Iterator<ParserDefinition> iterator;

        public ChainState() {
            super();
        }

        @SuppressWarnings("unchecked")
        public <T> T proceed(Object... arguments) throws Exception {
            T result = null;

            Object[] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                // TODO: validate type
                params[i] = (arguments.length > i
                                ? arguments[i] : parameters[i]);
            }

            if (iterator.hasNext()) {
                result = (T) iterator.next().invoke(params);
            }
            else {
                Method actual =
                    type.getMethod(invoked.getName(), invoked.getParameterTypes());
                result = (T) actual.invoke(definition.getInstance(), params);
            }

            return result;
        }

        /*

        definition
            invoke(chain)
                new Chain(chian, this).invoke

        Chain
            ctor(parent chain, definition)
                this(definitions[])

            ctor(definitions)
                proceed()

            proceed
                peek
                if null or state at bottom
                    if end of defs or at bottom
                        if parent
                            parent.proceed
                        else null
                    else
                        idx++
                        defs[idx].method.invoke(createProxy(Defs[idx].instance, this)
                        idx--
                else
                    state.proceed()

        ChainProxy
            invoke
                - @Around only related to immediate parser subclasses
                - since this will walk the tree automatically
                - ie: A extends B and C extends A maps the dependencies right
                -     away so we will chain A to B and then C to A
                new ChainState(super, @Around)
                push state
                state.proceed(chain)
                pop state
                return result

        ChainState
            proceed
                idx++
                if around exists && idx < len
                    definition.invoke(chain)
                else
                    super.proxy
                idx--
                return result
         */

        // TODO: if type A extends B and @Around, then A first depends
        //       if type C extends A [as well as B then] and @Around, then C first, followed by A

        public Object create(final Class<?> type) {
            instance = Enhancer.create(type, new InvocationHandler() {
                @Override
                public Object invoke(Object instance, Method method,
                                     Object[] params)
                    throws Throwable {

                    if (invoked == null) {
                        invoked = method;
                        parameters = params;
                        stack.push(ChainState.this);

                        Set<ParserDefinition> found = new TreeSet<ParserDefinition>();
                        ParserDefinition[] defs = parsers.getParsers(Around.class);
                        for (ParserDefinition def : defs) {
                            // TODO: check method params
                            if (def.getMethod().getName().equals(method.getName())) {
                                found.add(def);
                            }
                        }

                        iterator = found.iterator();
                    }
                    else {
                        // TODO: compare params
                        if (!invoked.getName().equals(method.getName())) {
                            throw new IllegalStateException("unknown");
                        }
                    }

                    Object result = proceed();
                    if (stack.peek() == ChainState.this) { stack.pop(); }
                    return result;
                }
            });

            return instance;
        }
    }


    @SuppressWarnings("unchecked")
    public <T> T create(T instance) {
        Class<T> clazz = (Class<T>) instance.getClass();
        return create(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> type) {
        ChainState state = new ChainState();
        state.create(type);
        return (T) state.instance;
    }

    public <T> T proceed(Object... arguments) {
        // peek / chain state / proceed()
        ChainState state = stack.peek();
        if (state != null) {
            try { return state.proceed(arguments); }
            catch (Exception e) {
                // TODO: better handle
                throw new IllegalStateException(e);
            }
        }
        else if (definitions != null) {
            // TODO: what is this?
            // return (T) definitions[index++].invoke(this, null);
        }
        else {
            throw new IllegalStateException("error");
        }

        return null;
    }
}
