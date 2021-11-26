package tests;


import org.fog.utils.PrivacyJsonHelper;
import org.junit.*;

import java.util.ArrayList;

public class TestStuff {


    @Test
    public void testJsonString(){
        int simulatedPath = 1337;
        int simulatedScenario = 1111;
        ArrayList<Integer> compromisedFogNodes = new ArrayList<>();
        compromisedFogNodes.add(1);
        compromisedFogNodes.add(2);
        compromisedFogNodes.add(3);
        compromisedFogNodes.add(4);
        compromisedFogNodes.add(5);


        PrivacyJsonHelper helper = new PrivacyJsonHelper(simulatedPath, simulatedScenario, compromisedFogNodes);

        helper.addEvent("hi",22, 32, 101);
        helper.addEvent("test",23, 32, 110);


        helper.writeJsonToFile("./TESTJSON.json");
    }

}
