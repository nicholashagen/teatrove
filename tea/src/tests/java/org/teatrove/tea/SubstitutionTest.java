package org.teatrove.tea;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.teatrove.tea.runtime.Substitution;

/*

TODO: SubstitutionTest
- class injector fails to load sub$0 and then outer class (have to use URL loader by itself)
- cannot mix shared var state w/ non-shared
- sub expr

*/

public class SubstitutionTest extends AbstractTemplateTest {
    
    public static class SubstitutionState {
        private Map<Substitution, Future<Object>> results = 
            new LinkedHashMap<Substitution, Future<Object>>();
        
        public SubstitutionState() {
            super();
        }
        
        public int findResult(int a) {
            return a * 5;
        }
        
        public String[] findObjects() {
            return new String[] { "TEST1", "TEST2" }; 
        }
        
        public List<String> findList() {
            return Arrays.asList(findObjects());
        }
        
        public Map<String, String> findMap() {
            return Collections.singletonMap("TEST", "VALUE");
        }
        
        public void add(Substitution sub, Future<Object> future) {
            this.results.put(sub, future);
        }
        
        public void join(int timeout, Substitution callback) {
            this.await(timeout);
            try { callback.substitute(this.getAll()); }
            catch (Exception e) { 
                throw new IllegalStateException(e); 
            }
        }
        
        public boolean await(long timeout) {
            long end = System.currentTimeMillis() + timeout;
            for (Future<Object> result : results.values()) {
                long remaining = end - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                
                try { result.get(remaining, TimeUnit.MILLISECONDS); }
                catch (Exception exception) { return false; }
            }
            
            return true;
        }
        
        public Object get(Substitution sub) {
            try { return results.get(sub).get(); }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
        
        public Object[] getAll() {
            int index = 0;
            Object[] values = new Object[results.size()];
            for (Future<Object> future : results.values()) {
                try { values[index++] = future.get(); }
                catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
            
            return values;
        }
    }
    
    public static class SubstitutionContext {
        private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool();
        
        public SubstitutionState fork(Substitution... subs) {
            SubstitutionState state = new SubstitutionState();
            for (final Substitution sub : subs) {
                Future<Object> result = 
                    EXECUTOR.submit(new Callable<Object>() {
                        public Object call() {
                            try { return sub.rsubstitute(); } 
                            catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    });
                
                state.add(sub, result);
            }

            return state;
        }
        
        public void join(SubstitutionState state, int timeout, 
                         Substitution callback) {
            state.await(timeout);
            try { callback.substitute(state.getAll()); }
            catch (Exception e) { 
                throw new IllegalStateException(e); 
            }
        }
    }

    /*
    public static interface Entity { }
    
    public static interface Player extends Entity {
        public int getId();
        public void setId(int id);
        
        public Team getTeam();
        public void setTeam(Team team);
    }
    
    public static interface Team extends Entity  {
        public int getId();
        public void setId(int id);
    }
    
    public static class PlayerEntity implements Player {
        private Integer id;
        private TeamEntity team;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public Team getTeam() { return this.team; }
        public void setTeam(Team team) { this.team = (TeamEntity) team; }
    }
    
    public static class TeamEntity implements Team {
        private Integer id;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
    }

    public static void main(String[] args) throws Exception {

        Class<?> entityClass = PlayerEntity.class;
        
        String className = entityClass.getName().concat("$JAXB");
        ClassFile classFile = new ClassFile(className);
        classFile.addDefaultConstructor();
        for (Constructor<?> ctor : entityClass.getConstructors()) {
            MethodInfo mi = classFile.addConstructor(ctor);
            CodeBuilder builder = new CodeBuilder(mi);
            builder.loadThis();
            for (LocalVariable var : builder.getParameters()) {
                builder.loadLocal(var);
            }
            
            builder.invokeSuperConstructor(mi.getMethodDescriptor().getParameterTypes());
            builder.returnVoid();
        }
        
        Modifiers mods = new Modifiers();
        mods.setPublic(true);
        
        for (Method method : entityClass.getMethods()) {
            String name = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            if (name.startsWith("set") && 
                method.getReturnType() == void.class &&
                paramTypes.length == 1 && 
                Entity.class.isAssignableFrom(paramTypes[0])) {
                
                String fieldName = 
                    Character.toLowerCase(name.charAt(3)) + name.substring(4);
                Field field = entityClass.getDeclaredField(fieldName);
                
                MethodInfo mi = classFile.addMethod
                (
                    mods, name, 
                    MethodDesc.forArguments
                    (
                        TypeDesc.VOID, TypeDesc.forClass(field.getType())
                    ), 
                    null
                );
                
                CodeBuilder builder = new CodeBuilder(mi);
                builder.loadThis();
                builder.loadLocal(builder.getParameters()[0]);
                builder.invokeSuper(method);
                builder.returnVoid();
            }
        }
        
        ClassInjector injector = ClassInjector.getInstance();
        OutputStream os = injector.getStream(className);
        classFile.writeTo(os);
        os.close();
        
        Class<?> clazz = injector.loadClass(className);
        System.out.println("CLASS: " + clazz);
        System.exit(1);
    }
    */
    
    public static void main(String[] args) throws Exception {
        
        // add contexts
        addContext(new SubstitutionContext());
        
        // execute template
        compile("util.frame");
        execute("substitution", "test");
    }
}
