package Solution;

import OOP.Provided.OOPAssertionFailure;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.lang.Class;
import java.util.*;

public class OOPUnitCore {

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

    public static OOPTestSummary runClass(Class<?> testClass){
        if(testClass == null || (!testClass.isAnnotationPresent(OOPTestClass.class))) {
            throw new IllegalArgumentException();
        }
        try {
            Object new_instance = testClass.getConstructor().newInstance();

            // get all the methods
            Method[] methods = testClass.getDeclaredMethods();

            // invoke methods with OOPSetup annotation
            for(Method m : methods){
                if(m.isAnnotationPresent(OOPSetup.class)){
                    new_instance.invoke(m, null);
                }
            }
            /*
            Method[] methods_with_OOPSetup_annotation = Arrays.stream(methods).filter(m->m.isAnnotationPresent(OOPSetup.class) != null)
                    .forEach()
                    .collect(Collectors.toList());
            for(Method m : methods){
                new_instance.invoke(m, null);
            }
           */
            // invoke methods with OOPBefore annotation
            for(Method m : methods){
                if(m.isAnnotationPresent(OOPBefore.class)){
                    if()
                    new_instance.invoke(m, null);
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


}
