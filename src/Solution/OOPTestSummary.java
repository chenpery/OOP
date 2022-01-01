package Solution;

import OOP.Provided.OOPResult;

import java.util.HashMap;
import java.util.Map;

public class OOPTestSummary {

    private Map<String, OOPResult> map;

    /* constructor */
    public OOPTestSummary (Map<String, OOPResult> testMap){
        this.map = testMap;
    }

    /* return total test that succeeded */
    public int getNumSuccesses(){
        int res = 0;
        for(OOPResult it : map.values()){
            if(it.getResultType() == OOPResult.OOPTestResult.SUCCESS){
                res++;
            }
        }
        return res;
    }

    /* return total test that failed because thrown of OOPAssertionError exception */
    public int getNumFailures() {
        int res = 0;
        for(OOPResult it : map.values()){
            if(it.getResultType() == OOPResult.OOPTestResult.FAILURE){
                res++;
            }
        }
        return res;
    }

    /* return total test that failed because thrown of unmatched expected exception*/
    public int getNumExceptionMismatches(){
        int res = 0;
        for(OOPResult it : map.values()){
            if(it.getResultType() == OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH){
                res++;
            }
        }
        return res;
    }

    /* return total test that failed because thrown else exception */
    public int getNumErrors(){
        int res = 0;
        for(OOPResult it : map.values()){
            if(it.getResultType() == OOPResult.OOPTestResult.ERROR){
                res++;
            }
        }
        return res;
    }


}
