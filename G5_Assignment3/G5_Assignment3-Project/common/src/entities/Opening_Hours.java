package entities;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the operating hours of the restaurant.
 * Includes regular weekly schedules and specific date exceptions.
 * Each time slot now tracks its active status to sync correctly with the database.
 */
public class Opening_Hours {

	/** Map to store standard weekly operating hours including their active status. */
	private Map<DayOfWeek, TimeRange> regularSchedule;

	/** Map to store specific date exceptions (holidays, events). */
	private Map<LocalDate, TimeRange> exceptionSchedule;

	public Opening_Hours() {
		this.regularSchedule = new HashMap<DayOfWeek, TimeRange>();
		this.exceptionSchedule = new HashMap<LocalDate, TimeRange>();
		initializeDefaultSchedule();
	}

	/**
	 * Inner class representing a time window and its operational status.
	 */
	public static class TimeRange {
		private LocalTime openTime;
		private LocalTime closeTime;
		private boolean isActive;
		private String description;
		
		public TimeRange(LocalTime openTime, LocalTime closeTime, boolean isActive, String description) {
			this.openTime = openTime;
			this.closeTime = closeTime;
			this.isActive = isActive;
			this.description = description;
		}

		public LocalTime getOpenTime() { return openTime; }
		public LocalTime getCloseTime() { return closeTime; }
		public boolean isActive() { return isActive; }
		public String getDescription() { return description; } 
		
		@Override
		public String toString() {
			if (!isActive) return "Closed (Inactive)";
			return openTime + " - " + closeTime;
		}
	}

	/**
	 * Sets the operating hours for a specific day of the week.
	 * @param day The day of the week.
	 * @param open Opening time.
	 * @param close Closing time.
	 * @param active Whether the restaurant is active on this day.
	 */
	public void setRegularHour(DayOfWeek day, LocalTime open, LocalTime close, boolean active) {
		regularSchedule.put(day, new TimeRange(open, close, active, null));
	}
	
	/**
	 * Compatibility method for active days.
	 */
	public void setRegularHour(DayOfWeek day, LocalTime open, LocalTime close) {
		setRegularHour(day, open, close, true);
	}

	/**
	 * Sets an exception for a specific date.
	 */
	public void setException(LocalDate date, LocalTime open, LocalTime close, String description) {
		exceptionSchedule.put(date, new TimeRange(open, close, true, description));
	}

	/**
	 * Core logic to determine if the restaurant is open at a given date and time.
	 * Priorities: Exceptions first, then regular schedule if active.
	 * @param dateTime The timestamp to check.
	 * @return true if open, false otherwise.
	 */
	public boolean isOpen(LocalDateTime dateTime) {
		LocalDate date = dateTime.toLocalDate();
		LocalTime time = dateTime.toLocalTime();

		// 1. Check exceptions (Holidays/Events)
		if (exceptionSchedule.containsKey(date)) {
			return isTimeInRange(time, exceptionSchedule.get(date));
		}

		// 2. Check regular schedule
		DayOfWeek day = date.getDayOfWeek();
		if (regularSchedule.containsKey(day)) {
			TimeRange range = regularSchedule.get(day);
			// Check if the day is marked as active in the system
			if (!range.isActive()) {
				return false;
			}
			return isTimeInRange(time, range);
		}

		return false;
	}

	/**
	 * Helper to check if a time falls within a range, considering midnight crossing.
	 */
	private boolean isTimeInRange(LocalTime time, TimeRange range) {
		if (range == null || !range.isActive()) return false;
		
		LocalTime open = range.openTime;
		LocalTime close = range.closeTime;

		// Standard case: 08:00 - 23:00
		if (close.isAfter(open)) {
			return (time.isAfter(open) || time.equals(open)) && time.isBefore(close);
		} 
		// Midnight crossing case: 20:00 - 03:00
		else {
			return (time.isAfter(open) || time.equals(open)) || time.isBefore(close);
		}
	}

	/**
	 * Sets default operating hours (09:00 - 23:00) for all days.
	 */
	private void initializeDefaultSchedule() {
		LocalTime open = LocalTime.of(9, 0);
		LocalTime close = LocalTime.of(23, 0);
		for (DayOfWeek day : DayOfWeek.values()) {
			setRegularHour(day, open, close, true);
		}
	}

	public Map<DayOfWeek, TimeRange> getRegularSchedule() {
		return regularSchedule;
	}

	public Map<LocalDate, TimeRange> getExceptionSchedule() {
		return exceptionSchedule;
	}
	
	/**
	 * Returns a string representation of the schedule.
	 * Uses manual loops to avoid Lambda expressions.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== Restaurant Operating Hours ===\n");

		DayOfWeek[] weekOrder = {
			DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
			DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
		};

		sb.append("--- Weekly Schedule ---\n");
		for (DayOfWeek day : weekOrder) {
			sb.append(String.format("%-10s: ", day.toString()));
			if (regularSchedule.containsKey(day)) {
				sb.append(regularSchedule.get(day).toString());
			} else {
				sb.append("Not Set");
			}
			sb.append("\n");
		}

		if (!exceptionSchedule.isEmpty()) {
			sb.append("\n--- Special Exceptions ---\n");
			// Using TreeMap to keep dates sorted without using Streams
			Map<LocalDate, TimeRange> sortedExceptions = new TreeMap<LocalDate, TimeRange>(exceptionSchedule);
			for (Map.Entry<LocalDate, TimeRange> entry : sortedExceptions.entrySet()) {
				sb.append("Date: ").append(entry.getKey()).append(" | Hours: ");
				if (entry.getValue() != null) {
					sb.append(entry.getValue().toString());
				} else {
					sb.append("Closed");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}