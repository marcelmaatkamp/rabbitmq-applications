package org.event.model.sensor.temperature;

import org.event.model.sensor.Sensor;
import org.event.model.sensor.SensorEvent;

/**
 * Created by marcelmaatkamp on 02/11/15.
 */
public class TemperatureSensorEvent extends SensorEvent {

    public TemperatureSensorEvent() {}

    private double temperature;

    public TemperatureSensorEvent(Sensor sensor, int index, double temperature) {
        super(sensor, index);
        this.temperature = temperature;
    }

    public TemperatureSensorEvent(double temperature) {
        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }


}
