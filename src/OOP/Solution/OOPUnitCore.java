package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.Class;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class OOPUnitCore {

//    private static Annotation[] ;

    /* if the input objects is not equal throw exception*/
    public static void assertEquals(Object expected, Object actual){
        if(!expected.equals(actual)){
            throw new OOPAssertionFailure(expected,actual);
        }
    }

    /* fail test method via trow exception */
    public static void fail(){
        throw new OOPAssertionFailure();
    }


    public static OOPTestSummary runClassHelper(Class<?> testClass, String tag){
        if(testClass == null || (!testClass.isAnnotationPresent(OOPTestClass.class))) {
            throw new IllegalArgumentException();
        }
        try {
            Object new_instance = testClass.getConstructor().newInstance();

            ArrayList<Method> setupMethods = new ArrayList<>();
            ArrayList<Method> beforeMethods = new ArrayList<>();
            ArrayList<Method> afterMethods = new ArrayList<>();

            // collect OOPSetup, OOPBefore, OOPAfter methods from father to sun
            Class<?> temp = new_instance.getClass();
            while( temp != null){
                Method[] methods = testClass.getDeclaredMethods(); // not including inherited methods
                for(Method m : methods) {
                    if (m.isAnnotationPresent(OOPSetup.class)) {
                        setupMethods.add(m);
                    }
                    if (m.isAnnotationPresent(OOPBefore.class)) {
                        beforeMethods.add(m);
                    }
                    if (m.isAnnotationPresent(OOPAfter.class)) {
                        afterMethods.add(m);
                    }
                }
                temp = temp.getSuperclass();
            }


            // collecting (and ordering) the Test-methods (with/without tag)
            OOPTestClass classAnnotation = testClass.getAnnotation(OOPTestClass.class);
            OOPTestClass.OOPTestClassType ordered = OOPTestClass.OOPTestClassType.UNORDERED;
            if(classAnnotation != null) {
                ordered = classAnnotation.value();
            }
            int i = 0;
            ArrayList<Method> testMethods = new ArrayList<>();
            Method[] thisClassMethods = testClass.getDeclaredMethods(); // not including inherited methods
            for(Method m : thisClassMethods){
                if(m.isAnnotationPresent(OOPTest.class)){
                    OOPTest annotation = m.getAnnotation(OOPTest.class);
                    if(annotation != null) {
                        if (Objects.equals(OOPTestClass.OOPTestClassType.ORDERED, ordered)) {
                            // get the right order for the method
                            i = annotation.order();
                        }
                        if (tag == null) {
                            testMethods.add(i,m);
                        }
                        else{
                            // get only tagged methods
                            String thisMethodTag = annotation.tag();
                            if (Objects.equals(thisMethodTag, tag)){
                                testMethods.add(i,m);
                            }
                        }
                        i++;
                    }
                }
            }

            // invoke OOPSetup methods
            Collections.reverse(setupMethods);
            for (Method m: setupMethods) {
                m.invoke(testClass, null);
            }

            // invoke OOPTest methods with OOPBefore & OOPAfter
            Collections.reverse(beforeMethods);
            for (Method m: testMethods) {
                // invoke OOPBefore that include the current method m
                for (Method bm: beforeMethods){
                    OOPBefore beforeAnnotation = bm.getAnnotation(OOPBefore.class);
                    String[] methodsArray = beforeAnnotation.value();
                    for (String str: methodsArray) {
                        if (str == m.getName()){
                            bm.invoke(testClass, null);
                            break;
                        }
                    }
                }
                // invoke current OOPTest method (m)
                m.invoke(testClass, null);
                // invoke OOPAfter that include the current method m
                for (Method am: afterMethods){
                    OOPAfter afterAnnotation = am.getAnnotation(OOPAfter.class);
                    String[] methodsArray = afterAnnotation.value();
                    for (String str: methodsArray) {
                        if (str == m.getName()){
                            am.invoke(testClass, null);
                            break;
                        }
                    }
                }
            }



        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    public static OOPTestSummary runClass(Class<?> testClass) {
        return runClassHelper(testClass, null);
    }


    public static OOPTestSummary runClass(Class<?> testClass, String tag) {
        return runClassHelper(testClass, tag);
    }


}
