package OOP.Solution;

import OOP.Provided.OOPResult;

public class OOPResultImpl implements OOPResult{

    private OOPTestResult res;
    private String message;


    /***** constructor ***********/
    public OOPResultImpl(OOPTestResult r, String msg){
        this.res = r;
        this.message = msg;
    }
    /**
     * @return the result type, which is one of four possible type. See OOPTestResult.
     */
    @Override
    public OOPResult.OOPTestResult getResultType(){
        return this.res;
    }

    /**
     * @return the message of the result in case of an error.
     */
    @Override
    public String getMessage(){
        return this.message;
    }

    /**
     * Equals contract between two test results.
     */
    @Override
    public boolean equals(Object obj){
        boolean result = false;
        if(obj instanceof OOPResult){
            OOPResult temp = (OOPResult) obj;
            result = (temp.getResultType() == this.getResultType()) && (temp.getMessage() == this.message);
        }
        return result;
    }

}
