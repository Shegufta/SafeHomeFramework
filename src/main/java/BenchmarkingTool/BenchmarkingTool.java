package BenchmarkingTool;

import Temp.DEV_ID;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 13-Sep-19
 * @time 2:16 PM
 */
public class BenchmarkingTool
{
    private boolean isGLOBALdevicesInitiated;
    private List<DEV_ID> localDevIdLIst = new ArrayList<>();

    public BenchmarkingTool()
    {
        this.isGLOBALdevicesInitiated = false;
        this.initiateLOCALdevIdList();
    }

    private void initiateLOCALdevIdList()
    {
        localDevIdLIst.add(DEV_ID.A);
        localDevIdLIst.add(DEV_ID.B);
        localDevIdLIst.add(DEV_ID.C);
        localDevIdLIst.add(DEV_ID.D);
        localDevIdLIst.add(DEV_ID.E);
        localDevIdLIst.add(DEV_ID.F);
        localDevIdLIst.add(DEV_ID.G);
        localDevIdLIst.add(DEV_ID.H);
        localDevIdLIst.add(DEV_ID.I);
    }

    public void initiateDevices(List<DEV_ID> globalDevIDlist)
    {
        assert(!localDevIdLIst.isEmpty());

        for(DEV_ID localDevID : this.localDevIdLIst)
        {
            if(!globalDevIDlist.contains(localDevID))
                globalDevIDlist.add(localDevID);
        }

        this.isGLOBALdevicesInitiated = true;
    }
}
