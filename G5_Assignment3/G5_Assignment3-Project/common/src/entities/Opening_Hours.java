package entities;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class Opening_Hours {

	private int id;

	// Map to store standard weekly operating hours.
	// Key: DayOfWeek (e.g., MONDAY), Value: TimeRange object.
	private Map<DayOfWeek, TimeRange> regularSchedule;

	// Map to store exceptions (holidays, special events).
	// Key: Specific Date (LocalDate), Value: TimeRange (or null if closed).
	private Map<LocalDate, TimeRange> exceptionSchedule;

	public Opening_Hours(int id) {
		this.id = id;
		// Initialize Maps to avoid NullPointerExceptions later
		this.regularSchedule = new HashMap<>();
		this.exceptionSchedule = new HashMap<>();

		// Populate the standard schedule upon object creation
		initializeDefaultSchedule();
	}

	public Opening_Hours() {
		this.regularSchedule = new HashMap<>();
		this.exceptionSchedule = new HashMap<>();
	}

	// --- Inner Helper Class to Represent a Time Window ---
	public static class TimeRange {
		private LocalTime openTime;
		private LocalTime closeTime;

		public TimeRange(LocalTime openTime, LocalTime closeTime) {
			this.openTime = openTime;
			this.closeTime = closeTime;
		}

		public LocalTime getOpenTime() {
			return openTime;
		}

		public LocalTime getCloseTime() {
			return closeTime;
		}

		@Override
		public String toString() {
			return openTime + " - " + closeTime;
		}
	}

	// Method to add or update standard operating hours for a specific day.
	public void setRegularHour(DayOfWeek day, LocalTime open, LocalTime close) {
		regularSchedule.put(day, new TimeRange(open, close));
	}

	// Method to set specific hours for an exceptional date (e.g., holiday hours).
	public void setException(LocalDate date, LocalTime open, LocalTime close) {
		exceptionSchedule.put(date, new TimeRange(open, close));
	}

	/**
	 * Core business logic to check if the restaurant is open at a specific moment
	 * in time. This method prioritizes exception schedules over regular schedules.
	 * * @param dateTime The specific date and time to check.
	 * 
	 * @return true if the restaurant is currently open, false otherwise.
	 */
	public boolean isOpen(java.time.LocalDateTime dateTime) {
		LocalDate date = dateTime.toLocalDate();
		LocalTime time = dateTime.toLocalTime();

		// 1. Check for specific date exceptions (High Priority Check)
		if (exceptionSchedule.containsKey(date)) {
			TimeRange range = exceptionSchedule.get(date);
			return isTimeInRange(time, range);
		}

		// 2. If no exception, check the regular weekly schedule
		DayOfWeek day = date.getDayOfWeek();
		if (regularSchedule.containsKey(day)) {
			TimeRange range = regularSchedule.get(day);
			return isTimeInRange(time, range);
		}

		// 3. If the day is not defined (e.g., Sunday if only weekdays are set), assume
		// closed.
		return false;
	}

	/**
	 * Helper method to determine if a given time falls within a specific TimeRange.
	 */
	private boolean isTimeInRange(LocalTime time, TimeRange range) {
		if (range == null)
			return false;
		// Checks if time is >= openTime AND < closeTime
		return (time.isAfter(range.openTime) || time.equals(range.openTime)) && time.isBefore(range.closeTime);
	}

	/**
	 * Initializes a default schedule (e.g., Mon-Thu open, Fri-Sat closed).
	 */
	private void initializeDefaultSchedule() {
		LocalTime open = LocalTime.of(9, 0);
		LocalTime close = LocalTime.of(23, 0);

		for (DayOfWeek day : DayOfWeek.values()) {
			// Assumes standard weekday operating hours
			if (day != DayOfWeek.FRIDAY && day != DayOfWeek.SATURDAY) {
				setRegularHour(day, open, close);
			}
		}
	}

	// --- Getters and Setters ---
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<DayOfWeek, TimeRange> getRegularSchedule() {
		return regularSchedule;
	}

	public Map<LocalDate, TimeRange> getExceptionSchedule() {
		return exceptionSchedule;
	}
}