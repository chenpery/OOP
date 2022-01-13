package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPResult;
import OOP.Provided.*;

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
    public static void assertEquals(Object expected, Object actual) {
        if (expected != null && !expected.equals(actual)) {
            throw new OOPAssertionFailure(expected, actual);
        }
        if (expected == null && actual != null){
            throw new OOPAssertionFailure(expected, actual);
        }
    }

    /* fail test method via trow exception */
    public static void fail() {
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
                        Constructor<?> ctorMethod = fValueClass.getDeclaredConstructor(fValueClass);
                        ctorMethod.setAccessible(true);
                        copy = ctorMethod.newInstance(fValue);
                    } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e2) {
                        copy = fValue;
                    }
                    //MAYBE NEED FINALLY HERE TOO ????????????????????????????????
                }
            } catch (Exception e) {
                return null;
            } finally {
                backedFields.add(copy);
            }
        }
        return backedFields;
    }

    public static OOPTestSummary runClassHelper(Class<?> testClass, String tag) {
        if (testClass == null || (!testClass.isAnnotationPresent(OOPTestClass.class))) {
            throw new IllegalArgumentException();
        }
        Map<String, OOPResult> mapResults = new HashMap<>();

        try {
            Constructor<?> ctor = testClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object new_instance = ctor.newInstance();

            ArrayList<Method> setupMethods = new ArrayList<>();
            ArrayList<Method> beforeMethods = new ArrayList<>();
            ArrayList<Method> afterMethods = new ArrayList<>();

            // collect OOPSetup, OOPBefore, OOPAfter methods from father to sun
            // and collecting OOPTest methods with/without ORDER/tag
            Class<?> className = new_instance.getClass();
            OOPTestClass classAnnotation = testClass.getAnnotation(OOPTestClass.class);
            OOPTestClass.OOPTestClassType ordered = OOPTestClass.OOPTestClassType.UNORDERED;
            if (classAnnotation != null) {
                ordered = classAnnotation.value();
            }
//            int i = 1;
            Map<Integer, Method> temp = new HashMap<>();
            //while (className != null) {
//                Method[] methods = className.getDeclaredMethods(); // not including inherited methods
                Method[] methods = className.getMethods();
                int k = 1;
                for (Method m : methods) {
                        if (m.isAnnotationPresent(OOPSetup.class)) {
//                            if (!setupMethods.contains(m)) {
                                setupMethods.add(m);
//                            }
                            continue;
                        }
                        if (m.isAnnotationPresent(OOPBefore.class)) {
                            beforeMethods.add(m);
                            continue;
                        }
                        if (m.isAnnotationPresent(OOPAfter.class)) {
                            afterMethods.add(m);
                            continue;
                        }
                        if (m.isAnnotationPresent(OOPTest.class)) {
                            OOPTest annotation = m.getAnnotation(OOPTest.class);
                            if (annotation != null) {
                                OOPTestClass.OOPTestClassType currentClassOrdered = OOPTestClass.OOPTestClassType.UNORDERED;
                                if (m.getDeclaringClass().getAnnotation(OOPTestClass.class) != null){
                                    currentClassOrdered = m.getDeclaringClass().getAnnotation(OOPTestClass.class).value();
                                }
//                            if (Objects.equals(OOPTestClass.OOPTestClassType.ORDERED, ordered)) {
                                if (Objects.equals(OOPTestClass.OOPTestClassType.ORDERED, currentClassOrdered)) {
                                    // get the right order for the method
                                    int i = methods.length + 1;
                                    i += annotation.order();
                                    if (tag == null) {
                                        temp.put(i, m);
                                    } else {
                                        // get only tagged methods
                                        String thisMethodTag = annotation.tag();
                                        if (thisMethodTag.contains(tag)) {
                                            temp.put(i, m);
                                        }
                                    }
                                } else {
                                    if (tag == null) {
                                        temp.put(k, m); //if not ordered, we have the initial j = nethods.legnth + 1
                                    } else {
                                        // get only tagged methods
                                        String thisMethodTag = annotation.tag();
                                        if (thisMethodTag.contains(tag)) {
                                            temp.put(k, m);
                                        }
                                    }
                                    k++;
                                }

                            }
                        }
                }
                //className = className.getSuperclass();
            //}
            Stream<Map.Entry<Integer, Method>> stream = temp.entrySet().stream().sorted(Map.Entry.comparingByKey());
            Map<Integer, Method> testMethods = stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));



            // invoke OOPSetup methods
            Collections.reverse(setupMethods);
            for (Method m : setupMethods) {
                m.invoke(new_instance, null);
            }


            // getting the OOPExceptionRule fields to check results later
            Field[] fields = testClass.getDeclaredFields();
            Field expectedException = null;
            for (Field f : fields) {
                if (f.isAnnotationPresent(OOPExceptionRule.class)) {
                    expectedException = f;
                    expectedException.setAccessible(true);
                    break;
                }
            }
            OOPExpectedException err = null;
            if (expectedException != null) err = (OOPExpectedException) expectedException.get(new_instance);


            // invoke OOPTest methods with OOPBefore & OOPAfter
//            boolean expectedExc_wasNull = false;
            Collections.reverse(beforeMethods);
            for (Method m : testMethods.values()) {

//                err = err.expect(null);
//                err = err.expectMessage("");

                OOPResult result = null;
                boolean failed_before_after = false;
                // invoke OOPBefore that include the current method m
                for (Method bm : beforeMethods) {
                    if (failed_before_after) break;
                    OOPBefore beforeAnnotation = bm.getAnnotation(OOPBefore.class);
                    String[] methodsArray = beforeAnnotation.value();
                    for (String str : methodsArray) {
                        if (Objects.equals(str, m.getName())) {
                            //backup the fields of new_instance (the object of our tested class) before invoking OOPBefore
                            ArrayList<Object> backedFields = backupFileds(new_instance, fields);
                            try {
                                bm.invoke(new_instance, null);
                                break;
                            } catch (Exception e) { // OOPBefore  method failed so need to restore the value of field
                                for (int j = 0; j < fields.length; j++) {
                                    fields[j].set(new_instance, (backedFields.get(j)));
                                }
                                failed_before_after = true;
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getClass().getName());
                                break;
                                // I think there should be another break for the outer loop (for the bm method)
                            }
                        }
                    }
                }

                if (failed_before_after) {
                    mapResults.put(m.getName(), result);
                } else {
                    failed_before_after = false;

                    try {
                        // invoke current OOPTest method (m)
                        m.invoke(new_instance, null);
                        if ( (err != null && err.getExpectedException() == null) || err == null) {
                            result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
                            mapResults.put(m.getName(), result);
                        } else {
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, err.getExpectedException().getName());
                                mapResults.put(m.getName(), result);
                        }
                    } catch (Exception e) {
                        Throwable noWarper = e.getCause(); // now we have the real exception the method threw
                        Class<?> expClass = noWarper.getClass();
                        if (expClass.equals(OOPAssertionFailure.class)) {
                            result = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE, e.getMessage());
                        } else {
                            if (err != null) {
                                err = (OOPExpectedException) expectedException.get(new_instance);
                                if (err.getExpectedException() == null) {
                                    result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, expClass.toString());
                                } else {
                                    if (expectedException.getType() == OOPExpectedException.class && err.assertExpected((Exception) noWarper)) {
                                        result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
                                    } else {
                                        OOPExceptionMismatchError mismatch = new OOPExceptionMismatchError(err.getExpectedException(), (Class<? extends Exception>) expClass);
                                        result = new OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, mismatch.getMessage());
                                    }
//                                if (err.getExpectedException() != e.getClass() || !err.assertExpected(e)) {
//                                    OOPExceptionMismatchError mismatch = new OOPExceptionMismatchError(err.getExpectedException(), e.getClass());
//                                    result = new OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, mismatch.getMessage());
//                                    mapResults.put(m.getName(), result);
//                                } else {
//                                    result = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
//                                    mapResults.put(m.getName(), result);
//                                }
                                }
                            } else {
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, expClass.toString());
                            }
                        }
//                        failed_before_after = true;
                        mapResults.put(m.getName(), result);
                    }
                }

                // invoke OOPAfter that include the current method m
                for (Method am : afterMethods) {
                    if (failed_before_after) break;
                    OOPAfter afterAnnotation = am.getAnnotation(OOPAfter.class);
                    String[] methodsArray = afterAnnotation.value();
                    for (String str : methodsArray) {
                        if (Objects.equals(str, m.getName())) {
                            //backup the fields of new_instance (the object of our tested class) before invoking OOPBefore
                            ArrayList<Object> backedFields = backupFileds(new_instance, fields);
                            try {
                                am.invoke(new_instance, null);

                                break;
                            } catch (Exception e) { // OOPBefore  method failed so need to restore the value of field
                                for (int j = 0; j < fields.length; j++) {
                                    fields[j].set(new_instance, (backedFields.get(j)));
                                }
                                result = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, "");
                                failed_before_after = true;
                                break;
                                // I think there should be another break for the outer loop (for the am loop)
                            }
                        }
                    }
                }
                if(result != null && failed_before_after) {
                    mapResults.put(m.getName(), result);
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
