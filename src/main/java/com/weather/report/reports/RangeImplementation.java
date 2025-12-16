package com.weather.report.reports;

import java.util.Objects;

public class RangeImplementation<T extends Comparable<? super T>> implements Report.Range<T>, Comparable<RangeImplementation<T>> {
    //Il tipo concreto usato come parametro di Range<T> dipende dal report (LocalDateTime, Duration oppure Double), ma è un tipo comparabile per far funzionare contains
    //nel caso del gateway report mi serve Duration, ma per semplificare il merge uso un parametro generico
    private final T start;
    private final T end;
    private final boolean isLast;
    
    public RangeImplementation(T start, T end, boolean isLast) {
        this.start = start;
        this.end = end;
        this.isLast = isLast;
    }


    @Override
    public int hashCode() {
        return Objects.hash(start, end, isLast);
    }

    @Override
    public int compareTo(RangeImplementation<T> o) {
        return this.start.compareTo(o.start);
    }

    @Override
    public boolean equals(Object obj) {
         if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        RangeImplementation<?> other = (RangeImplementation<?>) obj;
        return Objects.equals(start, other.start) &&
           Objects.equals(end, other.end) &&
           Objects.equals(isLast, other.isLast);
    }
    
    @Override
    public T getStart() {
        return start;
    }

    @Override
    public T getEnd() {
        return end;
    }

    @Override
    public boolean contains(T value) {
        if (value == null) return false;

        //in generale, un valore appartiene a un bucket se start ≤ v
        if (this.start.compareTo(value) > 0) return false;

        //in generale, un valore appartiene a un bucket se v < end 
        //se è l'ultimo valore, appartiene a un bucket se v ≤ end
        return (isLast) ? this.end.compareTo(value) >= 0 : this.end.compareTo(value) > 0;
    }

}
