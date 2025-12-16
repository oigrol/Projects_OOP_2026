package com.weather.report.reports;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

public class GatewayReportImplementation implements GatewayReport{
    //un report deve avere valori non più modificabili, solo leggibili con get
    private final String code;
    private final String startDate;
    private final String endDate;
    private final long numberOfMeasurements;
    private final Collection<String> mostActiveSensors;
    private final Collection<String> leastActiveSensors;
    private final Map<String, Double> sensorLoadRatio;
    private final Collection<String> outlierSensors;
    private final double batteryChargePercentage;
    private final SortedMap<Range<Duration>, Long> histogram;

    public GatewayReportImplementation(String code, String startDate, String endDate,
        long numberOfMeasurements, Collection<String> mostActiveSensors, Collection<String> leastActiveSensors,
        Map<String, Double> sensorLoadRatio, Collection<String> outlierSensor, double batteryChargePercentage, 
        SortedMap<Range<Duration>, Long> histogram) {
            this.code = code;
            this.startDate = startDate;
            this.endDate = endDate;
            this.numberOfMeasurements = numberOfMeasurements;
            this.mostActiveSensors = mostActiveSensors;
            this.leastActiveSensors = leastActiveSensors;
            this.sensorLoadRatio = sensorLoadRatio;
            this.outlierSensors = outlierSensor;
            this.batteryChargePercentage = batteryChargePercentage;
            this.histogram = histogram;
        }


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getStartDate() {
        return startDate;
    }

    @Override
    public String getEndDate() {
        return endDate;
    }

    @Override
    public long getNumberOfMeasurements() {
        return numberOfMeasurements;
    }

    @Override
    public Collection<String> getMostActiveSensors() {
        return mostActiveSensors;
    }

    @Override
    public Collection<String> getLeastActiveSensors() {
        return leastActiveSensors;
    }

    @Override
    public Map<String, Double> getSensorsLoadRatio() {
        return sensorLoadRatio;
    }

    @Override
    public Collection<String> getOutlierSensors() {
        return outlierSensors;
    }

    @Override
    public double getBatteryChargePercentage() {
        return batteryChargePercentage;
    }

    @Override
    public SortedMap<Range<Duration>, Long> getHistogram() {
        return histogram;
    }

}
