package com.jiushuo.uavRct.entity.mqtt;

import java.util.HashSet;

import dji.common.battery.BatteryCellVoltageLevel;
import dji.common.battery.BatteryOverview;
import dji.common.battery.ConnectionState;
import dji.common.battery.LowVoltageBehavior;
import dji.common.battery.PairingState;
import dji.common.battery.SelfHeatingState;
import dji.common.battery.WarningRecord;
import dji.common.error.DJIError;
import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.CalibrationOrientation;
import dji.common.flightcontroller.CompassCalibrationState;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.FlightWindWarning;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeAssessment;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.flightcontroller.GravityCenterState;
import dji.common.flightcontroller.LandingGearMode;
import dji.common.flightcontroller.LandingGearState;
import dji.common.flightcontroller.SmartRTHState;
import dji.common.flightcontroller.VisionLandingProtectionState;
import dji.common.flightcontroller.accesslocker.AccessLockerState;
import dji.common.flightcontroller.accesslocker.FormattingProgressState;
import dji.common.flightcontroller.adsb.AirSenseAirplaneState;
import dji.common.flightcontroller.adsb.AirSenseDirection;
import dji.common.flightcontroller.adsb.AirSenseWarningLevel;
import dji.common.flightcontroller.flightassistant.SmartCaptureAction;
import dji.common.flightcontroller.flightassistant.SmartCaptureFollowingMode;
import dji.common.flightcontroller.flightassistant.SmartCaptureSystemStatus;
import dji.common.flightcontroller.imu.CalibrationState;
import dji.common.flightcontroller.imu.MultipleOrientationCalibrationHint;
import dji.common.flightcontroller.imu.OrientationCalibrationState;
import dji.common.flightcontroller.imu.SensorState;
import dji.common.model.LocationCoordinate2D;

public class DroneState {
    //Aircraft类
    private String name;//可能是无人机的名字
    private Boolean isConnected;//无人机是否连接
    private String firmwarePackageVersion;//
    private String serialNumber;

    //1.FlightController类
    //1.1FlightControllerState类
    //1.1.1Flight Information
    private Boolean areMotorsOn;
    private Boolean isFlying;
    //LocationCoordinate3D类
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Float takeoffLocationAltitude;//Relative altitude of the aircraft home location relative to sea level, in meters.
    //Attitude类
    private Double pitch;
    private Double roll;//Current speed of the aircraft in the y direction, in meters per second, using the N-E-D (North-East-Down) coordinate system.
    private Double yaw;
    private Float velocityX;
    private Float velocityY;
    private Float velocityZ;
    private Integer FlightTimeInSeconds;
    //1.1.2Flight Actions
    private Boolean isLandingConfirmationNeeded;// if the clearance between the aircraft and the ground is less than 0.3m, and confirmation from the user is needed to continue the landing. When the confirmation is needed, confirmLanding in FlightController can be used to continue landing.
    //1.1.3Flight Mode
    private String flightMode;
    //1.1.4Sensors
    private Integer satelliteCount;//the GPS satellite count
    private GPSSignalLevel gpsSignalLevel;//枚举类型
    private Boolean isIMUPreheating;
    private Boolean isUltrasonicBeingUsed;
    private Float ultrasonicHeightInMeters;//A float value of the height of the aircraft measured by the ultrasonic sensor in meters.
    private Boolean doesUltrasonicHaveError;//if ultrasonic sensor has error.
    private Boolean isVisionPositioningSensorBeingUsed;//if a vision sensor is being used.Usually, the vision sensor works when the aircraft is less than 3m above ground.
    //1.1.5Flight Assistance
    private FlightOrientationMode flightOrientationMode;//枚举
    private Boolean isFailsafeEnabled;//if the signal is lost between remote controller and aircraft, and FailSafe is enabled.
    private BatteryThresholdBehavior batteryThresholdBehavior;//Recommended action based on remaining battery life.
    private Boolean isLowerThanBatteryWarningThreshold;
    private Boolean isLowerThanSeriousBatteryWarningThreshold;
    private FlightWindWarning flightWindWarning;//Warning related to high winds.
    private Integer flightCount;//The count of flights within the battery life cycle. Cleared when power-on.
    private Integer flightLogIndex;//The current index of the flight log on the aircraft. It is useful to find the corresponding flight log.
    //1.1.6Home
    private Boolean isHomeLocationSet;
    private LocationCoordinate2D homeLocation;//包括double类型的经度和维度
    private GoHomeAssessment goHomeAssessment;
    //GoHomeAssessment可以获取很多状态,包括：
    private Integer remainingFlightTime;//The estimated remaining time, in seconds
    private Integer timeNeededToGoHom;//The estimated time, in seconds, needed for the aircraft to go home from its current location.
    private Integer timeNeededToLandFromCurrentHeight;//The estimated time, in seconds, needed for the aircraft to move downward from its current position and land.
    private Integer batteryPercentageNeededToGoHome;//The estimated battery percentage, in the range of [0,100], needed for the aircraft to go home and have 10% battery remaining. This includes landing of the aircraft.
    private Integer batteryPercentageNeededToLandFromCurrentHeight;//The battery percentage, in the range of [0,100], needed for the aircraft to move downward from its current position and land.
    private Integer maxRadiusAircraftCanFlyAndGoHome;//The maximum radius, in meters, an aircraft can fly from its home location and still make it all the way back home, based on altitude, distance, battery, etc. If the aircraft goes out farther than the max radius, it will fly as far back home as it can and land. If the aircraft is using the simulator, this value will be 0.
    private SmartRTHState smartRTHState;//The Smart Return-To-Home (RTH) state for the current flight.
    private Integer smartRTHCountdown;//The countdown (in seconds) for the Smart Return-To-Home (RTH). Once the countdown reaches 0, the aircraft will execute an automatic go-home procedure. It is only valid when getSmartRTHState is COUNTING_DOWN.

    private GoHomeExecutionState goHomeExecutionState;//枚举
    private Boolean isGoingHome;
    private Integer goHomeHeight;//Gets the height when the aircraft is going home in meters.

    //1.2GravityCenterState类
    private GravityCenterState.GravityCenterCalibrationState gravityCenterCalibrationState;//枚举，The status of the gravity's calibration.
    private DJIError clibrationError;//Gets calibration errors. Returns null when gravity calibration is normal.
    //1.3Compass类
    private Float heading;//Represents the heading, in degrees. True North is 0 degrees, positive heading is East of North, and negative heading is West of North. Heading bounds are [-180, 180].
    private Boolean hasError;//true if the compass has an error. If true, the compass needs calibration.
    private Boolean isCalibrating;//true if the compass is currently calibrating.
    private CompassCalibrationState compassCalibrationState;//枚举
    //1.4RTK类
    private Boolean isRTKConnected;//true if RTK is connected to the aircraft.
    //1.4.1RTKState类：
    private Boolean isRTKBeingUsed;
    //RTK先跳过

    //1.5LandingGear类
    private LandingGearState landingGearState;//枚举
    private LandingGearMode landingGearMode;//枚举

    //1.6FlightAssistant类
    private Boolean collisionAvoidanceEnabled;//Gets collision avoidance status (enabled/disabled).
    private Boolean upwardsAvoidanceEnabled;//Gets upward avoidance status (enabled/disabled).
    private Boolean activeObstacleAvoidanceEnabled;//Gets active obstacle avoidance status (enabled/disabled).
    private Boolean smartCaptureEnabled;//Determines whether SmartCapture is enabled. When enabled, users can When enabled, deep learning gesture recognition allows the user to take selfies, record videos, and control the aircraft (GestureLaunch, Follow and GestureLand) using simple hand gestures. It is only supported when isSmartCaptureSupported returns true.
    private SmartCaptureFollowingMode smartCaptureFollowingMode;//枚举，Gets the following mode for SmartCapture. It is only valid when SmartCapture is enabled.
    private Boolean RTHObstacleAvoidanceEnabled;//Determines if Obstacle Avoidance is enabled during RTH.
    private Boolean RTHRemoteObstacleAvoidanceEnabled;//When it is enabled, the aircraft will adjust its RTH route automatically to avoid obstacles in far distance. The gimbal will not respond to any commands from the application or the remote controller.
    private Boolean visionAssistedPositioningEnabled;//Gets vision positioning status (enabled/disabled).
    private Boolean precisionLandingEnabled;//Gets precision landing status (enabled/disabled).
    private Boolean landingProtectionEnabled;//Gets landing protection status (enabled/disabled).
    //1.6.1VisionDetectionState类（未完全）
    private Boolean isSensorBeingUsed;// if the vision sensor is working.
    private Boolean isDisabled;//if the vision sensor is disabled.
    //1.6.2VisionControlState类
    private Boolean isBraking;//if the aircraft is braking automatically to avoid collision.
    private Boolean isAvoidingActiveObstacleCollision;// if the aircraft is avoiding collision from an obstacle moving towards the aircraft.
    private Boolean isAscentLimitedByObstacle;//if the aircraft will not ascend further because of an obstacle detected within 1m above it.
    private Boolean isPerformingPrecisionLanding;//if the aircraft is landing precisely.
    private VisionLandingProtectionState landingProtectionState;//Gets the aircraft's landing protection state. This status is valid when landing protection is enabled.
    private Boolean isAdvancedPilotAssistanceSystemActive;//if Advanced Pilot Assistance System (APAS) is active. When it is active, the aircraft will change flight path automatically to avoid obstacles.
    //1.6.3SmartCaptureState类
    private SmartCaptureSystemStatus smartCaptureSystemStatus;//The system status of SmartCapture.
    private SmartCaptureAction smartCaptureAction;//The executing SmartCapture action
    private SmartCaptureFollowingMode followingMode;//The following mode of SmartCapture.
    private Float photoCountdown;//The countdown for shooting photo.

    //1.7AccessLocker类
    private AccessLockerState accessLockerState;//Gets the latest state of the Access Locker.
    private Integer version;//Gets the version of the security feature.
    private String userAccount;//Returns the username of the current Access Locker account.
    //1.7.1FormattingState类
    private FormattingProgressState formattingProgressState;
    private DJIError DJIDataLockerError;//Checks if the formatting operation is successfully completed. The recent formatting operation failure if any. NULL if the recent formatting operation succeeded.
    //1.7.2UserAccountInfo类
    private String userName;//Gets the user name.
    private String securityCode;//Gets the security code of the account.
    //1.8Simulator类
    private Boolean isSimulatorActive;
    private Boolean flyZoneLimitationEnabled;//Gets fly zone system simulator status (enabled/disabled). By default, fly zone is disabled in the simulator.
    //1.8.1InitializationData类
    //1.8.2SimulatorState类：可以获取模拟环境中无人机的状态
    //1.9IMUState类
    private Integer IMUIndex;//The IMU's ID. Starts at 0.
    private SensorState gyroscopeState;//Returns the gyroscopic sensor's state value.
    private SensorState accelerometerState;//Returns the accelerometers sensor state value.
    private Integer calibrationProgress;//Returns the IMU's calibration progress, its range being [1, 100]. If the IMU is not calibrating, the value of the calibration progress will be -1.
    private CalibrationState calibrationState;//Returns the status of the IMU's calibration.
    private MultipleOrientationCalibrationHint multipleOrientationCalibrationHint;//For products that require the user to orient the aircraft during the IMU calibration, this method can be used to inform the user when each orientation is done.
    //1.9.1MultipleOrientationCalibrationHint类
    private OrientationCalibrationState orientationCalibrationState;
    private HashSet<CalibrationOrientation> orientationsToCalibrate;//Gets an array with the aircraft orientations that have not been calibrated yet. Each element is a CalibrationOrientation enum value.
    private HashSet<CalibrationOrientation> orientationsCalibrated;//Gets an array with the aircraft orientations that have been calibrated. Each element is a CalibrationOrientation enum value.
    //1.10FlightControlData类：Contains all the virtual stick control data needed to move the aircraft in all directions.
    private Float pitch1, roll1, yaw1, verticalThrottle;
    //1.11AirSenseSystemInformation类
    private AirSenseWarningLevel airSenseWarningLevel;//The overall system warning level. This will be the worst case of all individual aircraft warning levels.
    private AirSenseAirplaneState[] airplaneStates;//The state of all airplanes detected by DJI AirSense system.
    //1.11.1AirSenseAirplaneState类
    private String code;//The unique code (ICAO) of the airplane.
    private AirSenseWarningLevel warningLevel;//The warning level determined by DJI AirSense system
    private AirSenseDirection airSenseDirection;//The direction of the airplane relative to the DJI aircraft.
    private float heading1;//The heading of the airplane.(Compass也有个heading)
    private Integer distance;//The distance between the airplane and DJI aircraft in meters.
    //1.12LEDsSettings类
    private Boolean areFrontLEDsOn;
    private Boolean areRearLEDsOn;
    private Boolean isStatusIndicatorOn;
    //1.13Limits类：常量，This class provides max and min values for flight controller virtual stick.

    //2.Battery类
    private Integer[] cellVoltages;
    private Integer numberOfCells;//the number of battery cells.
    private Boolean isSmartBattery;
    private PairingState pairingState;//
    private Integer Level1CellVoltageThreshold;//Gets the Level 1 cell voltage threshold in mV. When the cell voltage of the battery crosses below the threshold, Level 1 behavior will be executed. The valid range is [3600, 4000] mV.
    private Integer Level2CellVoltageThreshold;//Gets the Level 2 cell voltage threshold in mV. When the cell voltage of the battery crosses below the threshold, Level 2 behavior will be executed.
    private LowVoltageBehavior level1CellVoltageBehavior;//枚举Gets the behavior that will be executed when the cell voltage crosses under the Level 1 threshold.
    private LowVoltageBehavior level2CellVoltageBehavior;//Sets the behavior that will be executed when the cell voltage crosses under the Level 2 threshold.
    private WarningRecord latestWarningRecord;//Gets the battery's most recent warning record. Anytime the battery experiences a significant exception, it will get recorded as a warning record.
    private WarningRecord[] WarningRecords;//Gets the battery's warning records, which are kept for 30 days using objects of type WarningRecord. Call the isSmartBattery() method before using this method. Not supported by Osmo and non-smart batteries.
    //1.1WarningRecord类
    private Boolean isCurrentOverloaded;// if the battery should be discharged due to a current overload.
    private Boolean isShortCircuited;// if the battery has been or is short circuited
    private Integer lowVoltageCellIndex;//Returns the index at which one of the cells in the battery is below the normal voltage. The first cell has an index of 0. -1 represents no battery cells under voltage
    private Boolean isOverHeated;// if the battery has overheated.
    private Boolean isLowTemperature;// if the battery has experienced a temperature that is too low.
    private Boolean isCustomDischargeEnabled;//if the battery has been configured to be discharged over a specific number of days. Once the battery is fully recharged, the battery will again discharge over the number of days set here. This process is cyclical.
    private Integer damagedCellIndex;//Returns the index at which one of the cells in the battery is damaged. The first cell has an index of 0. -1 represents no damaged battery cells.
    private Integer selfDischargeInDays;//Smart batteries can be setup to automatically discharge over a custom period of time. This method gets the battery's self discharge period in days with range [1, 10]. For Inspire 2 and M200 series products, the max range value can be 20. Not supported by non-smart batteries and the Osmo series.
    //1.2BatteryState类(setStateCallback获得)
    private Integer fullChargeCapacity;//Returns the total amount of energy, in mAh (milliamp hours), stored in the battery when the battery is fully charged. The energy of the battery at full charge changes over time as the battery continues to get used. Over time, as the battery continues to be recharged, the value of getFullChargeCapacity will decrease.
    private Integer chargeRemaining;//Returns the remaining energy stored in the battery in mAh (milliamp hours).
    private Integer hargeRemainingInPercent;//Returns the percentage of battery energy left with range [0, 100].
    private SelfHeatingState selfHeatingState;//Get self-heating state of the battery. When the temperature of the battery is below 6 celsius degrees, it will warm up automatically. It is only supported by Mavic 2 Enterprise.
    private Integer designCapacity;//Returns the design capacity of the battery in mAh (milliamp hours). It is the ideal capacity when the battery is new. This value will not change over time. It is only supported by smart battery.
    private Integer voltage;//Returns the current battery voltage (mV).
    private Integer current;//Returns the real time current draw of the battery (mA). A negative value means the battery is being discharged, and a positive value means it is being charged.
    private BatteryCellVoltageLevel cellVoltageLevel;//Current cell voltage level of the battery. It is only supported when the connected product is stand-alone A3 or N3.
    private Integer lifetimeRemaining;//Returns the battery's remaining lifetime as a percentage, with range [0, 100]. A new battery will be close to 100%. As a battery experiences charge/discharge cycles, the value will go down.
    private Float temperature;//Returns the battery's temperature, in Celsius, with range [-128, 127] degrees.
    private Integer numberOfDischarges;//Returns the total number of discharges the battery has gone through over its lifetime. The total number of discharges includes discharges that happen through normal use and discharges that are manually set.
    private ConnectionState connectionState;//Get battery connection status.
    //1.3AggregationState类(setAggregationStateCallback获得)
    private Integer NumberOfConnectedBatteries;//The number of currently connected batteries.
    private BatteryOverview[] BatteryOverviews;//Returns the overview of batteries in the battery group. When a battery is not connected, the isConnected property is false and the getChargeRemainingInPercent is zero. For Matrice 600, there are 6 elements in this array.
    //1.3.1BatteryOverview类
    private Integer batteryIndex;//ndex of the battery. Index starts from 0. For Matrice 600, the number 1 battery compartment relates to index 0.
    private Boolean isBatteryConnected;//true if the battery is currently connected to the aircraft.
    private Integer chargeRemainingInPercent;//The remaining percentage energy of the battery with range [0, 100].
    //end of BatteryOverview
    private Boolean isAnyBatteryDisconnected;//true if one of the batteries in the group is disconnected. When true, the aircraft is not allowed to take off.
    private Integer currentDrawAggregation;//Returns the real time current draw through the batteries. A negative value means the batteries are being discharged.
    private Integer currentVoltageAggregation;//Returns the current voltage (mV) provided by the battery group.
    private Boolean isVoltageDifferenceDetected;// if there is significant difference between the voltage (above 1.5V) of two batteries. When true, the aircraft is not allowed to take off.
    private Boolean isLowCellVoltageDetected;// if one of the batteries in the group has cells with low voltage. When true, the aircraft is not allowed to take off.
    private Boolean isCellDamaged;// if one of the batteries in the group has damaged cells. When true, the aircraft is not allowed to take off.
    private Boolean fullChargeCapacityAggregation;//Returns the the total amount of energy, in mAh (milliamp hours), stored in the batteries when the batteries are fully charged.
    private Integer chargeRemainingAggregation;//Returns the remaining energy stored in the batteries in mAh (milliamp hours).
    private Integer chargeRemainingInPercentAggregation;//Returns the percentage of energy left in the battery group with range [0,100].
    private Integer highestTemperature;//Returns the highest temperature (in Celsius) among the batteries in the group, with range [-128, 127] degrees.
    private Boolean isFirmwareDifferenceDetected;// if one of the batteries in the group has a firmware version different from the others. When it is true, the aircraft is not allowed to take off.

    //3.Camera类
    
}
