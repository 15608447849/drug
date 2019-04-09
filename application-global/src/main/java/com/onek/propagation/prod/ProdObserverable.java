package com.onek.propagation.prod;

public interface ProdObserverable {
    public void registerObserver(ProdObserver o);
    public void removeObserver(ProdObserver o);
    public void notifyObserver();
}
