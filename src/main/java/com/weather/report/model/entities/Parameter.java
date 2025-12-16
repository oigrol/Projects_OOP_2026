package com.weather.report.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/// A _parameter_ is a value associated with the gateway it belongs to.
/// 
/// It allows storing state or configuration information.
@Entity
public class Parameter {

  public static final String EXPECTED_MEAN_CODE = "EXPECTED_MEAN";
  public static final String EXPECTED_STD_DEV_CODE = "EXPECTED_STD_DEV";
  public static final String BATTERY_CHARGE_PERCENTAGE_CODE = "BATTERY_CHARGE";

  //per ogni Gateway devo avere più parametri ognuno identificato nel db da un id numerico
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; //univoco in tutta la tabella di parameter
  //Genero codici univoci all'interno del gateway
  //IDENTITY = uses database auto-increment (1, 2, 3, ...)

  //dato che due gateway diversi possono avere lo stesso parametro 'code' non posso usarlo per salvare in db (avrei più parameters con lo stesso code)
  //come id, ma uso un id generato che è sempre unico per l'inserimento in db di tutti i parameter

  //att: code deve essere univoco dato un gateway -> fai verifica se già presente in db
  private String code; //univoco solo nel gateway
  private String name;
  private String description;
  @Column(name = "param_value") //value è una parola riservata in sql 
  private double value;

  public Parameter() {
    // default constructor is needed by JPA
  }

  public Parameter(String code, String name, String description, double value) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.value = value;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

}
