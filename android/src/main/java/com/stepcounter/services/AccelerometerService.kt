package com.stepcounter.services

import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.facebook.react.bridge.WritableMap
import com.stepcounter.StepCounterModule
import com.stepcounter.utils.SensorFusionMath.dot
import com.stepcounter.utils.SensorFusionMath.norm
import com.stepcounter.utils.SensorFusionMath.normalize
import com.stepcounter.utils.SensorFusionMath.sum
import kotlin.math.min

class AccelerometerService(
        counterModule: StepCounterModule,
        sensorManager: SensorManager,
        ) : SensorListenService(counterModule, sensorManager) {
    override val sensorTypeString = "Accelerometer"
    override val sensorType = Sensor.TYPE_ACCELEROMETER
    override val detectedSensor: Sensor? = sensorManager?.getDefaultSensor(sensorType)
    override var currentSteps: Double = 0.0
    private var velocityRingCounter: Int = 0
    private var accelRingCounter: Int = 0
    private var oldVelocityEstimate: Float = 0f
    private var lastStepTimeNs: Long = 0

    // We want to keep a history of values to do a rolling average of the current
    private val accelRingX = FloatArray(ACCEL_RING_SIZE)
    private val accelRingY = FloatArray(ACCEL_RING_SIZE)
    private val accelRingZ = FloatArray(ACCEL_RING_SIZE)

    // We want to keep a history of values to do a rolling average of the current
    private val velocityRing = FloatArray(VELOCITY_RING_SIZE)

    /**
     * This function is responsible for updating the current steps.
     * All [values][android.hardware.SensorEvent.values] are in SI units (m/s^2)
     *
     * - values[0]: Acceleration minus Gx on the x-axis
     * - values[1]: Acceleration minus Gy on the y-axis
     * - values[2]: Acceleration minus Gz on the z-axis
     *
     * @param eventData array of vector.
     * @return If the current step is just updated
     * @see android.hardware.SensorEvent
     * @see android.hardware.SensorEvent.values
     * @see android.hardware.SensorEvent.timestamp
     */
    override fun updateCurrentSteps(eventData: FloatArray): Boolean {
        val timeNs = System.nanoTime()
        // First step is to update our guess of where the global z vector is.
        accelRingCounter++
        // We keep a rolling average of the last 50 values
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = eventData[0]
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = eventData[1]
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = eventData[2]
        // Next we'll calculate the average of the last 50 vectors in the ring
        val gravity: FloatArray = floatArrayOf(
            sum(accelRingX) / min(accelRingCounter, ACCEL_RING_SIZE),
            sum(accelRingY) / min(accelRingCounter, ACCEL_RING_SIZE),
            sum(accelRingZ) / min(accelRingCounter, ACCEL_RING_SIZE)
        )
        // Next step is to figure out the component of the current acceleration
        // in the direction of world_z and subtract gravity's contribution
        val currentZ: Float = dot(normalize(gravity), eventData) - norm(gravity)
        // Now we just need to update our estimate of the velocity
        velocityRingCounter++
        // We keep a rolling average of the last 10 values
        velocityRing[velocityRingCounter % VELOCITY_RING_SIZE] = currentZ
        // Calculate the average of the last 10 values
        val velocityEstimate: Float = sum(velocityRing)
        // If the velocity estimate is greater than the threshold and the previous
        val isWalkingOrRunning: Boolean =
            velocityEstimate > STEP_THRESHOLD &&
                oldVelocityEstimate <= STEP_THRESHOLD &&
                timeNs - lastStepTimeNs > STEP_DELAY_NS
        if (isWalkingOrRunning) {
            currentSteps = currentSteps.plus(1)
            Log.d(TAG_NAME, "STATUS: $currentSteps steps. TIMESTAMP: $timeNs")
            lastStepTimeNs = timeNs
        }
        oldVelocityEstimate = velocityEstimate
        return isWalkingOrRunning
    }

    companion object {
        /**
         * The delay between steps in nanoseconds
         */
        private const val STEP_DELAY_NS = 250000000 // 250ms

        /**
         * The size of the acceleration sensor data ring
         */
        private const val ACCEL_RING_SIZE = 50

        /**
         * The size of the acceleration's velocity ring
         */
        private const val VELOCITY_RING_SIZE = 10

        /**
         * The minimum acceleration that is considered a step
         */
        private const val STEP_THRESHOLD = 12f // 4f-16f

        val TAG_NAME: String = AccelerometerService::class.java.name
    }
}
