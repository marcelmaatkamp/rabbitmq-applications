package org.datadiode.model.event.sensor.temperature;

import org.datadiode.model.event.sensor.Sensor;
import org.datadiode.model.event.sensor.SensorEvent;

/**
 * Created by marcelmaatkamp on 02/11/15.
 */
public class TemperatureSensorEvent extends SensorEvent {

    private double temperature;

    public TemperatureSensorEvent(Sensor sensor, int index, double temperature) {
        super(sensor,index);
        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }


}
