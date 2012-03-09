package org.teatrove.tea;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.teatrove.trove.classfile.ClassFile;
import org.teatrove.trove.classfile.CodeBuilder;
import org.teatrove.trove.classfile.FieldInfo;
import org.teatrove.trove.classfile.LocalVariable;
import org.teatrove.trove.classfile.MethodDesc;
import org.teatrove.trove.classfile.MethodInfo;
import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.classfile.TypeDesc;
import org.teatrove.trove.util.ClassInjector;

public class InnerClassTest {

    public static void main(String[] args) throws Exception {
        ClassFile clazz = new ClassFile("Test");
        clazz.addDefaultConstructor();
        
        MethodInfo mi = clazz.addInitializer();
        CodeBuilder builder2 = new CodeBuilder(mi);
        builder2.loadStaticField("java.lang.System", "out", TypeDesc.forClass(System.out.getClass()));
        builder2.loadThisClass();
        builder2.invoke(Class.class.getMethod("toString"));
        builder2.invoke(System.out.getClass().getMethod("println", String.class));
        builder2.returnVoid();
        
        Modifiers mods = new Modifiers();
        mods.setPublic(true);

        // String getName() { return "World"; }
        MethodInfo name =
            clazz.addMethod(mods, "getName", MethodDesc.forArguments(TypeDesc.STRING, null), null);
        CodeBuilder nameBuilder = new CodeBuilder(name);
        nameBuilder.loadConstant("World");
        nameBuilder.returnValue(TypeDesc.STRING);

        // void test() { ... }
        MethodInfo method =
            clazz.addMethod(mods, "test", MethodDesc.forArguments(TypeDesc.VOID, null), null);
        CodeBuilder builder = new CodeBuilder(method);
        
        // final String greeting = "Hello";
        LocalVariable var = builder.createLocalVariable("greeting", TypeDesc.STRING);
        builder.loadConstant("Hello");
        builder.storeLocal(var);

        // new Runnable()
        LocalVariable[] finals = { var }; 
        ClassFile inner = clazz.addInnerClass("1");
        inner.addInterface(Runnable.class);
        
        Modifiers pvtMods = new Modifiers();
        pvtMods.setFinal(true);
        pvtMods.setPrivate(true);
        
        FieldInfo[] fields = new FieldInfo[finals.length + 1];
        TypeDesc[] params = new TypeDesc[finals.length + 1];
        params[0] = clazz.getType();
        fields[0] = inner.addField(pvtMods, "this$1", clazz.getType());
        for (int i = 0; i < finals.length; i++) {
            params[i + 1] = finals[i].getType();
            fields[i + 1] =
                inner.addField(pvtMods, "val$" + finals[i].getName(), finals[i].getType());
        }
        
        // Ctor
        MethodInfo ctor = inner.addConstructor(mods, params);
        CodeBuilder ctorBuilder = new CodeBuilder(ctor);
        ctorBuilder.loadThis();
        ctorBuilder.invokeSuperConstructor(null);
        for (int i = 0; i < fields.length; i++) {
            ctorBuilder.loadThis();
            ctorBuilder.loadLocal(ctorBuilder.getParameters()[i]);
            ctorBuilder.storeField(fields[i].getName(), fields[i].getType());
        }
        ctorBuilder.returnVoid();
        
        // Runnable.run()
        MethodInfo runnable = 
            inner.addMethod(mods, "run", MethodDesc.forArguments(TypeDesc.VOID, null), null);
        CodeBuilder runBuilder = new CodeBuilder(runnable);
        
        // System.out.print(greeting);
        runBuilder.loadStaticField("java.lang.System", "out", TypeDesc.forClass(System.out.getClass()));
        runBuilder.loadThis();
        runBuilder.loadField("val$greeting", TypeDesc.STRING);
        runBuilder.invoke(System.out.getClass().getMethod("print", String.class));
        
        // System.out.print(": ");
        runBuilder.loadStaticField("java.lang.System", "out", TypeDesc.forClass(System.out.getClass()));
        runBuilder.loadConstant(": ");
        runBuilder.invoke(System.out.getClass().getMethod("print", String.class));
        
        // System.out.print(Test.this.getName());
        runBuilder.loadStaticField("java.lang.System", "out", TypeDesc.forClass(System.out.getClass()));
        runBuilder.loadThis();
        runBuilder.loadField("this$1", clazz.getType());
        runBuilder.invokeVirtual(clazz.getClassName(), "getName", TypeDesc.STRING, null);
        runBuilder.invoke(System.out.getClass().getMethod("print", String.class));
        
        // System.out.println()
        runBuilder.loadStaticField("java.lang.System", "out", TypeDesc.forClass(System.out.getClass()));
        runBuilder.invoke(System.out.getClass().getMethod("println"));
        
        // End
        runBuilder.returnVoid();
        
        // new Runnable() { }
        builder.newObject(inner.getType());
        builder.dup();
        
        builder.loadThis();
        for (int i = 0; i < finals.length; i++) {
            builder.loadLocal(finals[i]);
        }
        
        builder.invokeConstructor(inner.getClassName(), params);
        
        // runnable.run()
        builder.invoke(Runnable.class.getMethod("run"));
        builder.returnVoid();
        
        // generate and test
        FileOutputStream output = new FileOutputStream("Test.class");
        clazz.writeTo(output);
        output.close();
        
        FileOutputStream output2 = new FileOutputStream("Test$1.class");
        inner.writeTo(output2);
        output2.close();
        
        ClassInjector injector = ClassInjector.getInstance();
        
        OutputStream cos = injector.getStream("Test");
        clazz.writeTo(cos);
        cos.close();
        
        OutputStream cos2 = injector.getStream("Test$1");
        inner.writeTo(cos2);
        cos2.close();
        
        Class<?> testClass = injector.loadClass("Test");
        testClass.getMethod("test").invoke(testClass.newInstance());
    }
}
