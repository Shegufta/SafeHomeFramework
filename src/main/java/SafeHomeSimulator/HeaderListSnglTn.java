/**
 * LockTable for SafeHome
 *
 * LockTable maintains a per-device lineage: the planned transition order of
 * that device’s lock, which is noted as Lineage Table in the SafeHome paper.
 * For each device, LockTable maintains a temporal plan of when the device
 * will be acquired by concerned routines. It shows which routine will be
 * executed on each device at each time point.
 *
 * @author Shegufta Ahsan
 * @project SafeHomeFramework
 * @date 17-Oct-19
 * @time 11:53 PM
 *
 *       Paper: Home, SafeHome: Smart Home Reliability with Visibility and
 *              Atomicity (Eurosys 2021)
 *     Authors: Shegufta Bakht Ahsan*, Rui Yang*, Shadi Abdollahian Noghabi^,
 *              Indranil Gupta*
 * Institution: *University of Illinois at Urbana-Champaign,
 *              ^Microsoft Research
 *
 */

package SafeHomeSimulator;

import java.util.HashMap;
import java.util.Map;

/**

 */
public class HeaderListSnglTn
{
    public static Map<CONSISTENCY_TYPE, String> CONSISTENCY_HEADER;

    private HeaderListSnglTn()
    {
        CONSISTENCY_HEADER = new HashMap<>();

        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.SUPER_STRONG, "SUPER_GSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.STRONG, "GSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.RELAXED_STRONG, "PSV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.EVENTUAL, "EV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.WEAK, "WV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY, "LV");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_FCFS, "LAZY_FCFS");
        CONSISTENCY_HEADER.put(CONSISTENCY_TYPE.LAZY_PRIORITY, "LAZY_PRIORITY");
    }

    private static HeaderListSnglTn singleton;

    public static synchronized HeaderListSnglTn getInstance()
    {
        if(null == HeaderListSnglTn.singleton)
        {
            HeaderListSnglTn.singleton = new HeaderListSnglTn();
        }

        return HeaderListSnglTn.singleton;
    }
}
