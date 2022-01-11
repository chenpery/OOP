package OOP.Solution;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;


public class OOPExpectedExceptionImpl implements OOPExpectedException {

    private Class<? extends Exception> expect_exception = null;
    private String message = "";


    /**
     * @return the expected exception type.
     */
    @Override
    public Class<? extends Exception> getExpectedException() {
        return this.expect_exception;
    }

    /**
     * expect an exception with the given type to be thrown.
     *
     * @param expected - the expected exception type.
     * @return this object.
     */
    @Override
    public OOPExpectedException expect(Class<? extends Exception> expected) {
        this.expect_exception = expected;
        return this;
    }

    /**
     * expect the exception message to have a message as its substring.
     * Should be okay if the message expected is a substring of the entire exception message.
     * Can expect several messages.
     * Example: for the exception message: "aaa bbb ccc", for an OOPExpectedException e:
     * e.expectMessage("aaa").expectMessage("bb c");
     * - This should be okay.
     *
     * @param msg - the expected message.
     * @return this object.
     */
    @Override
    public OOPExpectedException expectMessage(String msg){
        this.message = msg;
        return this;
    }

    /**
     * checks that the exception that was thrown, and passed as parameter,
     * is of a type as expected. Also checks expected message are contained in the exception message.
     * <p>
     * Should handle inheritance. For example, for expected exception A, if an exception B was thrown,
     * and B extends A, then it should be okay.
     *
     * @param e - the exception that was thrown.
     * @return whether or not the actual exception was as expected.
     */
    @Override
    public boolean assertExpected(Exception e){
        if(e == null) return false;
        //at first, e will be warped with InvocationTargetException
//        Throwable noWarper = e.getCause(); // now we have the real exception the method threw

        Class<?> expClass = e.getClass();
        while( expClass != null){
            if( expClass == this.expect_exception){
                if(e.getMessage() == null){
                    if(this.message == "") return true;
                    else return false;
                }
                if(e.getMessage().contains(this.message)){
                    return true;
                }
            }
            expClass = expClass.getSuperclass();
        }
        return false;
    }
    /**
     * @return an instance of an ExpectedException with no exception or expected messages.
     * <p>
     * This static method must be implemented in OOPExpectedExceptionImpl and return an actual object.
     * <p>
     * If this is the state of the ExpectedException object, then no exception is expected to be thrown.
     * So, if an exception is thrown, an OOPResult with ERROR should be returned
     */
    public static OOPExpectedExceptionImpl none() { //TODO
        return new OOPExpectedExceptionImpl();
    }
}