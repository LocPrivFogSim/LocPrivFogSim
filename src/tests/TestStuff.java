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

        helper.addEvent(22, "TestEvent1", 101);
        helper.addEvent(23, "TestEvent2", 110);
        helper.addEvent(24, "TestEvent3", 120);
        helper.addEvent(25, "TestEvent4", 130);
        helper.addEvent(26, "TestEvent5", 140);


        helper.writeJsonToFile("./TESTJSON.json");
    }

}
