/**
 * Device State in SafeHome.
 *
 * Device state currently does not impact how SafeHome models work, but it
 * is a potential aspect that could be further explored.
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

public enum DEV_STATE {
  ALARM,
  CLOSED,
  FIRE,
  FIREALARM,
  NIGHTALARM,
  OFF,
  ON,
  OPEN,
  SIRENCONTINUOUS,
  SIRENOFF,
  SIRENSOFF,
  STATE0,
  STATE1,
  STATE2,
  STATE3,
  STATE4,
  STATE5,
  STATE7,
  STATE8,
  STATE9,
  UNKNOWN,
  email
}
