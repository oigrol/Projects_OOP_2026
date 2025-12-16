package com.weather.report.operations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Parameter;
import com.weather.report.model.entities.User;
import com.weather.report.reports.GatewayReport;
import com.weather.report.reports.GatewayReportImplementation;
import com.weather.report.reports.RangeImplementation;
import com.weather.report.reports.Report.Range;
import com.weather.report.repositories.GatewayRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.repositories.UserRepository;
import com.weather.report.services.AlertingService;

public class GatewayOperationsImplementation implements GatewayOperations{

    private static final int BUCKETS_NUMBER = 20;
    private static final String GATEWAY_CODE_FORMAT = "GW_\\d{4}";

    private final GatewayRepository gatewayRepository = new GatewayRepository();
    //non serve invece una repo di parameter, perchè i parametri esistono solo in relazione ai gateway non indipendentemente
    private final UserRepository userRepository = new UserRepository(); //utile per controllare i ruoli
    private final MeasurementRepository measurementRepository = new MeasurementRepository();

    @Override
    public Gateway createGateway(String code, String name, String description, String username)
            throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
        checkMaintainerUser(username);
        checkGatewayCodeNotNull(code);
        checkCodeFormat(code);

        //verifico unicità Gateway
        if (gatewayRepository.read(code) != null) {
            throw new IdAlreadyInUseException("Il gateway " + code + " esiste già");
        }
        
        //creo nuovo Gateway
        Gateway gateway = new Gateway(code, name, description, username);
        gatewayRepository.create(gateway);
        return gateway;
    }

    @Override
    public Gateway updateGateway(String code, String name, String description, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        checkMaintainerUser(username);
        checkGatewayCodeNotNull(code);

        Gateway gateway = getGatewayIfExist(code);

        //aggiorno campi Gateway
        gateway.setName(name);
        gateway.setDescription(description);
        gateway.setModifiedBy(username);
        gateway.setModifiedAt(LocalDateTime.now());

        gatewayRepository.update(gateway);
        return gateway;        
    }

    @Override
    public Gateway deleteGateway(String code, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        checkMaintainerUser(username);
        checkGatewayCodeNotNull(code);

        Gateway gateway = getGatewayIfExist(code);

        // elimino Gateway esistente
        gatewayRepository.delete(code);
        AlertingService.notifyDeletion(username, code, Gateway.class);

        return gateway;
    }

    @Override
    public Collection<Gateway> getGateways(String... gatewayCodes) {
        //nelle operazioni di lettura non devo verificare che l'utente sia MAINTAINER -> va bene anche se ha permessi come viewer
        if (gatewayCodes == null || gatewayCodes.length == 0) {
            return gatewayRepository.read();
        }
        Set<String> gatewaysCodesSet = Arrays.stream(gatewayCodes)
            .collect(Collectors.toSet()); //mi permette di usare contains
        return gatewayRepository.read().stream()
            .filter(g -> gatewaysCodesSet.contains(g.getCode()))
            .toList();
    }

    @Override
    public Parameter createParameter(String gatewayCode, String code, String name, String description, double value,
            String username)
            throws IdAlreadyInUseException, InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        checkMaintainerUser(username);
        checkGatewayCodeNotNull(gatewayCode);
        checkParameterCodeNotNull(code);

        Gateway gateway = getGatewayIfExist(gatewayCode);

        //verifico unicità Parameter all'interno del Gateway
        if (gateway.getParameter(code) != null) {
            throw new IdAlreadyInUseException("Il parameter " + code + " associato al gateway " + gatewayCode + " esiste già");
        }

        //creo nuovo Parameter
        Parameter parameter = new Parameter(code, name, description, value);
        gateway.addParameter(parameter);
        gateway.setModifiedBy(username);
        gateway.setModifiedAt(LocalDateTime.now());

        gatewayRepository.update(gateway);
        return parameter;
    }

    @Override
    public Parameter updateParameter(String gatewayCode, String code, double value, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        checkMaintainerUser(username);
        checkGatewayCodeNotNull(gatewayCode);
        checkParameterCodeNotNull(code);

        Gateway gateway = getGatewayIfExist(gatewayCode);

        //verifico esistenza Parameter all'interno del Gateway
        Parameter parameter = gateway.getParameter(code);
        if (parameter == null) {
            throw new ElementNotFoundException("Il parameter " + code + " relativo al gateway " + gatewayCode + " non esiste");
        }

        //aggiorno campi Parameter
        parameter.setValue(value);
        gateway.setModifiedBy(username);
        gateway.setModifiedAt(LocalDateTime.now());

        gatewayRepository.update(gateway);
        return parameter;
    }

    @Override
    public GatewayReport getGatewayReport(String code, String startDate, String endDate)
            throws ElementNotFoundException, InvalidInputDataException {
        checkGatewayCodeNotNull(code);
        Gateway gateway = getGatewayIfExist(code);

        LocalDateTime startLocalDate = parseLocalDateTime(startDate, LocalDateTime.MIN);
        LocalDateTime endLocalDate = parseLocalDateTime(endDate, LocalDateTime.MAX);

        List<Measurement> measurements = getFilteredMeasurements(code, startLocalDate, endLocalDate);
        int numberOfMeasurements = measurements.size(); //numero totale di misurazioni del Gateway nell’intervallo richiesto

        Parameter batteryChargePercentageP = gateway.getParameter(Parameter.BATTERY_CHARGE_PERCENTAGE_CODE);
        double batteryChargePercentage = (batteryChargePercentageP != null) ? batteryChargePercentageP.getValue() : 0.0;

        /* se non ci sono misurazioni ritorno immediatamente liste e mappe vuote */
        if (numberOfMeasurements == 0) {
            return new GatewayReportImplementation(code, startDate, endDate, numberOfMeasurements, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>(), batteryChargePercentage, new TreeMap<>());
        }

        Collection<String> mostActiveSensors = new ArrayList<>();
        Collection<String> leastActiveSensors = new ArrayList<>();
        Map<String, Double> sensorsLoadRatio = new HashMap<>();
        setCollectionOfSensors(measurements, numberOfMeasurements, mostActiveSensors, leastActiveSensors, sensorsLoadRatio);

        Collection<String> outlierSensors = getOutlierSensors(measurements, gateway);

        SortedMap<Range<Duration>, Long> histogram = getHistogram(measurements, numberOfMeasurements);
        
        return new GatewayReportImplementation(code, startDate, endDate, numberOfMeasurements, mostActiveSensors, leastActiveSensors, sensorsLoadRatio, outlierSensors, batteryChargePercentage, histogram);
    }

    //helper di validazione per rendere il codice più leggibile

    /**
     * Check that the gateway code is not null
     * @param code gateway code
     * @throws InvalidInputDataException if cose is missing
     */
    private void checkGatewayCodeNotNull(String code) throws InvalidInputDataException {
        if (code == null) {
            throw new InvalidInputDataException("Codice gateway mancante");
        }    
    }

    /**
     * Check that the parameter code is not null
     * @param code parameter code
     * @throws InvalidInputDataException if code is missing
     */
    private void checkParameterCodeNotNull(String code) throws InvalidInputDataException {
        if (code == null) {
            throw new InvalidInputDataException("Codice parameter mancante");
        }    
    }

    /**
     * Check that the code is a String starting with "GW_" 
     * and followed by 4 decimal digits
     * @param code gateway code
     * @throws InvalidInputDataException if code is invalid or non-conforming
     */
    private void checkCodeFormat(String code) throws InvalidInputDataException {
        if (!code.matches(GATEWAY_CODE_FORMAT)) {
            throw new InvalidInputDataException("Il formato del codice del gateway (" + code + ") non è corretto");
        }    
    }

    /**
     * Retrieve gateway if it exists, otherwise 
     * throw an exception
     * @param code gateway code
     * @return the gateway 
     * @throws ElementNotFoundException if gateway is not contained in the system
     */
    private Gateway getGatewayIfExist(String code) throws ElementNotFoundException {
        Gateway gateway = gatewayRepository.read(code);
        if (gateway == null) {
            throw new ElementNotFoundException("Gateway " + code + " non trovato");
        }
        return gateway;
    }

    /**
     * Check that the user has permissions as a maintainer user
     * @param username user's username
     * @throws InvalidInputDataException if username is missing
     * @throws UnauthorizedException if username does not correspond to any existing user, or corresponds to a user who does not have the required permissions to execute the operation.
     */
    private void checkMaintainerUser(String username) throws InvalidInputDataException, UnauthorizedException {
        if (username == null) throw new InvalidInputDataException("Codice username mancante");
        User user = userRepository.read(username);
        if (user == null || user.getType() != UserType.MAINTAINER) {
            throw new UnauthorizedException("L'username " + username + " non corrisponde ad alcun utente esistente o l'utente non ha i permessi necessari per eseguire l'operazione");
        }
    }

    //metodi helper per rendere il metodo getGatewayReport più leggibile

    /**
     * Retrieve an instance of LocalDateTime from a text string using a specific formatter
     * @param date date with string format
     * @param defaultDate if date is null, there is no limit
     * @return date in LocalDateTime format
     * @throws InvalidInputDataException if date is not in correct format "yyyy-MM-dd HH:mm:ss"
     */
    private LocalDateTime parseLocalDateTime(String date, LocalDateTime defaultDate) throws InvalidInputDataException {
        if (date == null) return defaultDate; //max ~ +inf | min ~ -inf
        try {
            return LocalDateTime.parse(date, WeatherReport.DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidInputDataException("Il formato della data deve essere: " + WeatherReport.DATE_TIME_FORMATTER);
        }
    }

    /**
     * Retrieve a list of gateway measurements in the requested time interval
     * @param gatewayCode the code of the gateway
     * @param startDate the start date
     * @param endDate the end date
     * @return a list of gateway measurements between startDate and endDate
     */
    private List<Measurement> getFilteredMeasurements(String gatewayCode, LocalDateTime startDate, LocalDateTime endDate) {
        return measurementRepository.read().stream()
                .filter(m -> m.getGatewayCode().equals(gatewayCode)) //filtra solo le misurazioni di quel gateway
                .filter(m -> !m.getTimestamp().isBefore(startDate)) //filtra solo le misurazioni avvenute a partire da [start,..
                .filter(m -> !m.getTimestamp().isAfter(endDate)) //filtra solo le misurazioni avvenute fino a ...end]
                .toList();
    }

    /**
     * Analyze sensor data and collect it into related collections according to requests
     * @param measurements gateway's measurement
     * @param numberOfMeasurements number of gateway's measurement
     * @param mostActiveSensors list containing the sensors with the highest number of measurements
     * @param leastActiveSensors list containing the sensors with the least number of measurements
     * @param sensorsLoadRatio map containing the sensors with the relative percentage of measurements taken by the single sensor compared to the total of the gateway
     */
    private void setCollectionOfSensors(List<Measurement> measurements, int numberOfMeasurements, Collection<String> mostActiveSensors, Collection<String> leastActiveSensors, Map<String, Double> sensorsLoadRatio) {
        //ricavo una map che raggruppa le misurazioni per sensore e le conta
        Map<String, Long> countMeasurementsForSensor = measurements.stream()
                .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.counting()));

        //ricavo il massimo e il minimo numero di misurazioni per i sensori
        long maxCount = countMeasurementsForSensor.values().stream().max(Long::compareTo).orElse((long)0);
        long minCount = countMeasurementsForSensor.values().stream().min(Long::compareTo).orElse((long)0);

        countMeasurementsForSensor.forEach((sensorCode, measurementCount) -> {
            if (measurementCount == maxCount) mostActiveSensors.add(sensorCode);
            if (measurementCount == minCount) leastActiveSensors.add(sensorCode);
            double ratio = (double) measurementCount / numberOfMeasurements;
            sensorsLoadRatio.put(sensorCode, ratio);
        });
    }

    /**
     * Retrieve a list of sensorCodes whose average detected values ​​are anomalous, comparing the real average with the gateway's expected values
     * @param measurements gateway's measurement
     * @param gateway gateway
     * @return a list of outlier sensors
     */
    private Collection<String> getOutlierSensors(List<Measurement> measurements, Gateway gateway) {
        Collection<String> outlierSensors = new ArrayList<>();

        Parameter expectedMeanP = gateway.getParameter(Parameter.EXPECTED_MEAN_CODE);
        Parameter expectedStdDevP = gateway.getParameter(Parameter.EXPECTED_STD_DEV_CODE);

        if (expectedMeanP!=null && expectedStdDevP!= null) {
            double expectedMean = expectedMeanP.getValue();
            double expectedStdDev = expectedStdDevP.getValue();
            //raggruppo misurazioni per sensore e ne calcolo la media
            Map<String, Double> meanMeasurementsForSensor = measurements.stream()
                .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.averagingDouble(Measurement::getValue)));

            meanMeasurementsForSensor.forEach((sensorCode, sensorMean) -> {
                if (checkIfOutlier(expectedMean, expectedStdDev, sensorMean)) outlierSensors.add(sensorCode);
            });
        }

        return outlierSensors;
    }

    /**
     * Check if a sensor code is considered outlier
     * @param expected_mean expected mean
     * @param expected_std_dev expected standard deviation
     * @param sensor_mean average of the values ​​detected by a sensor
     * @return true if the average of the values ​​detected by a sensor is anomalous
     */
    private boolean checkIfOutlier(double expected_mean, double expected_std_dev, double sensor_mean) {
        return Math.abs(sensor_mean - expected_mean) >= 2.0 * expected_std_dev;
    }

    /**
     * Retrieve the histogram of the inter-arrival times between consecutive gateway measurements in the requested interval.
     * @param measurements gateway's measurement
     * @param numberOfMeasurements number of gateway's measurement
     * @return histogram with the duration count for each bucket
     */
    private SortedMap<Range<Duration>, Long> getHistogram(List<Measurement> measurements, int numberOfMeasurements) {
        SortedMap<Range<Duration>, Long> histogram = new TreeMap<>();

        if (numberOfMeasurements < 2) return histogram;

        //ordino le misurazioni in ordine cronologico
        List<Measurement> sortedMeasurements = measurements.stream().sorted(Comparator.comparing(Measurement::getTimestamp)).toList();

        //calcolo tutte le differenze temporali tra misurazioni consecutive
        List<Duration> interArrivalDurations = new ArrayList<>();
        for (int i=0; i<numberOfMeasurements-1; i++) {
            LocalDateTime start = sortedMeasurements.get(i).getTimestamp();
            LocalDateTime end = sortedMeasurements.get(i+1).getTimestamp();
            interArrivalDurations.add(Duration.between(start, end));
        }

        //calcolo i 20 intervalli contigui in cui suddividere il range di Duration
        Duration minDuration = interArrivalDurations.stream().min(Duration::compareTo).orElse(Duration.ZERO);
        Duration maxDuration = interArrivalDurations.stream().max(Duration::compareTo).orElse(Duration.ZERO);
        Duration range = maxDuration.minus(minDuration);
        Duration bucketRange = range.dividedBy(BUCKETS_NUMBER);

        for (int i=0; i<BUCKETS_NUMBER; i++) {
            //creo i 20 bucket impostando start e end di ogni bucket e un flag che dice se è l'ultimo bucket (end coincide con maxDuration)
            Duration start = minDuration.plus(bucketRange.multipliedBy(i)); //min + (bucketRange * i)
            boolean isLast = (i == BUCKETS_NUMBER - 1);
            Duration end = (isLast) ? maxDuration : minDuration.plus(bucketRange.multipliedBy(i+1)); //min + (bucketRange * (i+1)) se non è l'ultimo

            RangeImplementation<Duration> bucket = new RangeImplementation<>(start, end, isLast);

            //ogni bucket è un intervallo dell'istogramm -> count è il numero di duration di quel bucket
            long count = interArrivalDurations.stream().filter(d -> bucket.contains(d)).count();

            histogram.put(bucket, count);
        }
        
        return histogram;
    }

}
