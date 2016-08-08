package cn.pengbo.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HttpRequestUtils {


    public static void setParamMap(String key,String value,Map<String,String[]> params) {
        List<String> paramValues = null;
        String[] strings = params.get(key);
        if(strings == null){
            paramValues = new ArrayList<String>();
        }else {
            paramValues =new ArrayList<String>(Arrays.asList(strings)) ;
        }

        paramValues.add(value);

        String[] paramArr =  new String[paramValues.size()];
        for(int i = 0 ;i < paramArr.length ; i++) {
            paramArr[i] = paramValues.get(i);
        }
        params.put(key,paramArr);
    }


}
