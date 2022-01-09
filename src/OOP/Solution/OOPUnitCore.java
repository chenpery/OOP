package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.Class;
import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;
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



    private static ArrayList<Object> backupFileds(Object new_instance, Field[] fields) {
        ArrayList<Object> backedFields = new ArrayList<>();
        for (Field f : fields) {
            f.setAccessible(true);
            Object copy = null;
            try {
                Object fValue = f.get(new_instance); //get the value of the field
                Class<?> fValueClass = fValue.getClass(); // get the value class
                try {
                    Method cloneMethod = fValueClass.getMethod("clone", null);
                    copy = cloneMethod.invoke(fValue);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e1) { // no clone method or it failed
                    //try to use copy c'tor
                    try {
                        Constructor<?> ctorMethod = fValueClass.getConstructor(fValueClass);
                        ctorMethod.setAccessible(true);
                        copy = ctorMethod.newInstance(fValue);
                    } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e2) {
                        copy = fValue;
                    }
                    //MAYBE NEED FINALLY HERE TOO ????????????????????????????????
                }
            } catch (Exception e) {
                return null;
            }
            finally {
                backedFields.add(copy);
            }
        }
        return backedFields;
    }

    public static OOPTestSummary runClassHelper(Class<?> testClass, String tag){
        if(testClass == null || (!testClass.isAnnotationPresent(OOPTestClass.class))) {
            throw new IllegalArgumentException();
        }
        Map<String, OOPResult> mapResults = new HashMap<>();

        try {
            Constructor<?> ctor = testClass.getConstructor();
            ctor.setAccessible(true);
            Object new_instance = ctor.newInstance();

            ArrayList<Method> setupMethods = new ArrayList<>();
            ArrayList<Method> beforeMethods = new ArrayList<>();
            ArrayList<Method> afterMethods = new ArrayList<>();

            // collect OOPSetup, OOPBefore, OOPAfter methods from father to sun
            Class<?> className = new_instance.getClass();
            while( className != null){
                Method[] methods = className.getDeclaredMethods(); // not including inherited methods
                for(Method m : methods) {
                    if (m.isAnnotationPresent(OOPSetup.class)) {
                        setupMethods.add(m);
                        continue;
                    }
                    if (m.isAnnotationPresent(OOPBefore.class)) {
                        beforeMethods.add(m);
                        continue;
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
            int i = 1;
            Map<Integer,Method> temp = new HashMap<>();
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
                            temp.put(i,m); //if not ordered, we have the initial i = 0
                        }
                        else{
                            // get only tagged methods
                            String thisMethodTag = annotation.tag();
                            if (Objects.equals(thisMethodTag, tag)){
                                temp.put(i,m);
                            }
                        }
                        i++;
                    }
                }
            }
            Stream<Map.Entry<Integer,Method>> stream = temp.entrySet().stream().sorted(Map.Entry.comparingByKey());
            Map<Integer,Method> testMethods = stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


            // invoke OOPSetup methods
            Collections.reverse(setupMethods);
            for (Method m: setupMethods) {
                m.invoke(new_instance, null);
            }


            // getting the OOPExceptionRule fields to check results later
            Field[] fields = testClass.getDeclaredFields();
            Field expectedException = null;
            for (Field f: fields ) {
                if (f.isAnnotationPresent(OOPExceptionRule.class)){
                    expectedException = f;
                    break;
                }
            }


            // invoke OOPTest methods with OOPBefore & OOPAfter
            Collections.reverse(beforeMethods);
            OOPResult result = null;
            for (Method m: testMethods.values()) {
                // invoke OOPBefore that include the current method m
                for (Method bm: beforeMethods){
                    OOPBefore beforeAnnotation = bm.getAnnotation(OOPBefore.class);
                    String[] methodsArray = beforeAnnotation.value();
                    for (String str: methodsArray) {
                        if (Objects.equals(str, m.getName())){
                             //backup the fields of new_instance (the object of our tested class) before invoking OOPBefore
                            ArrayList<Object> backedFields = backupFileds(new_instance, fields);
                            try {
                                bm.invoke(new_instance, null);
                                break;
                            }
                            catch(Exception e){ // OOPBefore  method failed so need to restore the value of field
                                for (int j = 0; j< fields.length; j++) {
                                    fields[j].set(new_instance,(backedFields.get(j)));
                                }
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, "");
                                mapResults.put(m.getName(),result);
                                break;
                                // I think there should be another break for the outer loop (for the bm method)
                            }
                        }
                    }
                }
                try {
                    // invoke current OOPTest method (m)
                    m.invoke(new_instance, null);
                    result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
                }
                catch (OOPAssertionFailure e){
                    result = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE, e.getMessage());
                }
                catch(Exception e) {
                    if (OOPAssertionFailure.class.equals(e.getCause().getClass())) {
                        result = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE, e.getMessage());
                    } else {
                        if (expectedException != null) {
                            expectedException.setAccessible(true);

                            OOPExpectedExceptionImpl err = (OOPExpectedExceptionImpl) expectedException.get(new_instance);
                            if (err.getExpectedException() == null) {
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getClass().toString());
                            } else {
//                            OOPExpectedExceptionImpl f = (OOPExpectedExceptionImpl) expectedException.get(new_instance);
                                if (!err.assertExpected(e)) {
                                    OOPExceptionMismatchError mismatch = new OOPExceptionMismatchError(err.getExpectedException(), e.getClass());
                                    result = new OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, mismatch.getMessage());
                                } else{
                                    result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
                                }
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
                            //backup the fields of new_instance (the object of our tested class) before invoking OOPBefore
                            ArrayList<Object> backedFields = backupFileds(new_instance, fields);
                            try {
                                am.invoke(new_instance, null);
                                break;
                            }
                            catch(Exception e){ // OOPBefore  method failed so need to restore the value of field
                                for (int j = 0; j< fields.length; j++) {
                                    fields[j].set(new_instance,(backedFields.get(j)));
                                }
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, "");
                                mapResults.put(m.getName(),result);
                                break;
                                // I think there should be another break for the outer loop (for the am loop)
                            }
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
