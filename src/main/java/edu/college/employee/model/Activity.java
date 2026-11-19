package edu.college.employee.model;
import java.time.Instant;
public record Activity(String id,Instant at,String actor,String action,String target){ }
