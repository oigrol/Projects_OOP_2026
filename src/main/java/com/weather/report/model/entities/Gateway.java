package com.weather.report.model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import com.weather.report.model.Timestamped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/// A _gateway_ groups multiple devices that monitor the same physical quantity.  
/// 
/// It can be configured through parameters that provide information about its state or values needed for interpreting the measurements.
@Entity
public class Gateway extends Timestamped {
  @Id
  private String code; 

  private String name;
  private String description;

  //tutti i salvataggi applicati al gateway sono applicati anche ai parametri -> cascade
  //quando carico un gateway, carica anche tutti i suoi parametri figli -> fetch
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "gateway_code") //inserisce colonna di riferimento a gateway per sapere a chi si riferiscono i parametri
  private Collection<Parameter> parameters = new ArrayList<>();

  public Gateway() {
    // default constructor is needed by JPA
  }

  public Gateway(String code, String name, String description, String username) {
    this.code = code;
    this.name = name;
    this.description = description;
    //salvo metadati di Timestamped per tracciare creazione e modifica gatewat
    this.setCreatedBy(username);
    this.setCreatedAt(LocalDateTime.now());
    this.setModifiedBy(null);
    this.setModifiedAt(null);
  }

  public Collection<Parameter> getParameters() {
    return parameters;
  }

  public Parameter getParameter(String codeParameter) {
    return this.parameters.stream()
      .filter(p -> p.getCode().equals(codeParameter))
      .findFirst().orElse(null);
  }

  public void addParameter(Parameter parameter) {
    this.parameters.add(parameter);
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

}
