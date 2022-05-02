 using Xunit;

 public class TestTrackingAttackController
{
 [Fact]
    public void TestFileNameSplitting()
    {
        string filePath = @"C:\Users\lspie\Desktop\LocPrivFogSim\experiment_analysis\input\Test\output_3_20_13.json";
        TrackingAttackController th = TrackingAttackController.Instance;
        int[] x =  th.getExperimentParamsFromFileName(filePath);

        Assert.True(x[0] == 3); //strat
        Assert.True(x[1] == 20); //rate
        Assert.True(x[2] == 13);   //iteration
    }
}