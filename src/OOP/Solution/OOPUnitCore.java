package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.Class;
import java.util.*;
import java.util.stream.Stream;

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
        Map<String, OOPResult> mapResults = new HashMap<>();

        try {
            Object new_instance = testClass.getConstructor().newInstance();

            ArrayList<Method> setupMethods = new ArrayList<>();
            ArrayList<Method> beforeMethods = new ArrayList<>();
            ArrayList<Method> afterMethods = new ArrayList<>();

            // collect OOPSetup, OOPBefore, OOPAfter methods from father to sun
            Class<?> className = new_instance.getClass();
            while( className != null){
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
                className = className.getSuperclass();
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


//            Stream<Method> streamBefore = beforeMethods.stream();
//            Stream<Method> afterBefore = afterMethods.stream();
//            Stream<Method> streamTest = testMethods.stream();
//            streamTest = streamTest.map( m -> {})

            // getting the OOPExceptionRule fields to check results later
            Field[] fields = testClass.getDeclaredFields();
            Field expectedException = null;
            for (Field e: fields ) {
                if (e.isAnnotationPresent(OOPExceptionRule.class)){
                    expectedException = e;
                    break;
                }
            }
//            Stream<Field> streamExceptions = Arrays.stream(fields);
//            streamExceptions = streamExceptions.filter(f -> f.isAnnotationPresent(OOPExceptionRule.class));

            // invoke OOPTest methods with OOPBefore & OOPAfter
            Collections.reverse(beforeMethods);
            OOPResult result = null;
            for (Method m: testMethods) {
                // invoke OOPBefore that include the current method m
                for (Method bm: beforeMethods){
                    OOPBefore beforeAnnotation = bm.getAnnotation(OOPBefore.class);
                    String[] methodsArray = beforeAnnotation.value();
                    for (String str: methodsArray) {
                        if (Objects.equals(str, m.getName())){
                            bm.invoke(testClass, null);
                            break;
                        }
                    }
                }
                try {
                    // invoke current OOPTest method (m)
                    m.invoke(testClass, null);
                    result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
                }
                catch (OOPAssertionFailure e){
                    result = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE, e.getMessage());
                }
                catch(Exception e){
                    if (expectedException != null && expectedException.get(new OOPExpectedExceptionImpl()).equals(OOPExpectedExceptionImpl.none())){
                        result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getClass().toString());
                    }
                    else {
                        if (expectedException != null) {
                            OOPExpectedExceptionImpl f = (OOPExpectedExceptionImpl) expectedException.get(new OOPExpectedExceptionImpl());
                            if (f.assertExpected(e)) {
                                OOPExceptionMismatchError mismatch = new OOPExceptionMismatchError(f.getExpectedException(), e.getClass());
                                result = new OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, mismatch.getMessage());
                            }
                        }
                    }
                }
                // invoke OOPAfter that include the current method m
                for (Method am: afterMethods){
                    OOPAfter afterAnnotation = am.getAnnotation(OOPAfter.class);
                    String[] methodsArray = afterAnnotation.value();
                    for (String str: methodsArray) {
                        if (Objects.equals(str, m.getName())){
                            am.invoke(testClass, null);
                            break;
                        }
                    }
                }
                if (result != null){
                    mapResults.put(m.getName(),result);
                }
            }

            return new OOPTestSummary(mapResults);


        } catch (InvocationTargetException e) {
            System.out.println("InvocationTargetException");
            e.printStackTrace();
            return new OOPTestSummary(mapResults);
        } catch (InstantiationException e) {
            System.out.println("InstantiationException");
            e.printStackTrace();
            return new OOPTestSummary(mapResults);
        } catch (IllegalAccessException e) {
            System.out.println("IllegalAccessException");
            e.printStackTrace();
            return new OOPTestSummary(mapResults);
        } catch (NoSuchMethodException e) {
            System.out.println("NoSuchMethodException");
            e.printStackTrace();
            return new OOPTestSummary(mapResults);
        }
    }


    public static OOPTestSummary runClass(Class<?> testClass) {
        return runClassHelper(testClass, null);
    }

    public static OOPTestSummary runClass(Class<?> testClass, String tag) {
        return runClassHelper(testClass, tag);
    }


}
