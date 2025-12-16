package com.weather.report.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class PersistenceManager {
  private static final String TEST_PU_NAME = "weatherReportTestPU";
  private static final String PU_NAME = "weatherReportPU";

  private static EntityManagerFactory factory;
  private static String currentPUName = PersistenceManager.PU_NAME;

  private static final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(()->false);
  private static final ThreadLocal<EntityManager> currentManager = ThreadLocal.withInitial(()->null);

  public static void setTestMode() {
    if (factory != null && factory.isOpen()) {
      factory.close();
      factory = null;
    }
    currentPUName = PersistenceManager.TEST_PU_NAME;
  }

  private static EntityManagerFactory getCurrentFactory() {
    if (factory == null || !factory.isOpen()) {
      factory = Persistence.createEntityManagerFactory(currentPUName);
    }
    return factory;
  }

  public static EntityManager getEntityManager() {
    EntityManager currentEm = currentManager.get();
    if(currentEm == null || !currentEm.isOpen()){
      IO.println("*** new EntityManager");
      currentEm = getCurrentFactory().createEntityManager();
      currentManager.set(currentEm);
    }
    return currentEm;
  }

  public static void closeEntityManager(){
    EntityManager currentEm = currentManager.get();
    if(currentEm!=null && currentEm.isOpen() && !inTransaction.get()){
      currentEm.close();
      currentManager.remove();
    }
  }

  public static void close() {
    if (factory != null && factory.isOpen()) {
      factory.close();
    }
  }
}
