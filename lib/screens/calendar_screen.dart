import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:googleapis/calendar/v3.dart' as google_calendar;
import 'package:intl/intl.dart';
import '../services/google_calendar_service.dart';
import '../services/microsoft_calendar_service.dart';

/// Écran du calendrier visuel
/// Affiche un calendrier avec tous les événements Google et Microsoft
class CalendarScreen extends StatefulWidget {
  final GoogleCalendarService googleCalendarService;
  final MicrosoftCalendarService microsoftCalendarService;

  const CalendarScreen({
    super.key,
    required this.googleCalendarService,
    required this.microsoftCalendarService,
  });

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  CalendarFormat _calendarFormat = CalendarFormat.month;
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;

  Map<DateTime, List<CalendarEvent>> _events = {};
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _selectedDay = _focusedDay;
    _loadEventsForMonth(_focusedDay);
  }

  /// Charge les événements du mois
  Future<void> _loadEventsForMonth(DateTime month) async {
    setState(() => _isLoading = true);

    final firstDay = DateTime(month.year, month.month, 1);
    final lastDay = DateTime(month.year, month.month + 1, 0, 23, 59, 59);

    final newEvents = <DateTime, List<CalendarEvent>>{};

    // Charger événements Google si connecté
    if (widget.googleCalendarService.isSignedIn) {
      final googleEvents = await widget.googleCalendarService.getEventsBetween(
        firstDay,
        lastDay,
      );

      if (googleEvents != null) {
        for (var event in googleEvents) {
          final date = _getEventDate(event.start?.dateTime ?? event.start?.date);
          if (date != null) {
            newEvents.putIfAbsent(date, () => []).add(
              CalendarEvent(
                title: event.summary ?? 'Sans titre',
                start: event.start?.dateTime ?? event.start?.date,
                end: event.end?.dateTime ?? event.end?.date,
                location: event.location,
                source: 'Google',
                color: Colors.blue,
              ),
            );
          }
        }
      }
    }

    // Charger événements Microsoft si connecté
    if (widget.microsoftCalendarService.isSignedIn) {
      final microsoftEvents = await widget.microsoftCalendarService.getEventsBetween(
        firstDay,
        lastDay,
      );

      if (microsoftEvents != null) {
        for (var event in microsoftEvents) {
          final date = _getEventDate(event.start);
          if (date != null) {
            newEvents.putIfAbsent(date, () => []).add(
              CalendarEvent(
                title: event.subject,
                start: event.start,
                end: event.end,
                location: event.location,
                source: 'Microsoft',
                color: Colors.orange,
              ),
            );
          }
        }
      }
    }

    setState(() {
      _events = newEvents;
      _isLoading = false;
    });
  }

  /// Extrait la date (jour uniquement) d'un DateTime
  DateTime? _getEventDate(DateTime? dateTime) {
    if (dateTime == null) return null;
    return DateTime(dateTime.year, dateTime.month, dateTime.day);
  }

  /// Récupère les événements pour un jour donné
  List<CalendarEvent> _getEventsForDay(DateTime day) {
    final normalizedDay = DateTime(day.year, day.month, day.day);
    return _events[normalizedDay] ?? [];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Calendrier'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _loadEventsForMonth(_focusedDay),
            tooltip: 'Actualiser',
          ),
        ],
      ),
      body: Column(
        children: [
          // Indicateur de chargement
          if (_isLoading)
            const LinearProgressIndicator()
          else
            const SizedBox(height: 4),

          // Calendrier
          TableCalendar<CalendarEvent>(
            firstDay: DateTime.utc(2020, 1, 1),
            lastDay: DateTime.utc(2030, 12, 31),
            focusedDay: _focusedDay,
            selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
            calendarFormat: _calendarFormat,
            eventLoader: _getEventsForDay,
            startingDayOfWeek: StartingDayOfWeek.monday,
            locale: 'fr_FR',

            // Style du calendrier
            calendarStyle: CalendarStyle(
              markersMaxCount: 3,
              markerDecoration: BoxDecoration(
                color: Colors.blue.shade700,
                shape: BoxShape.circle,
              ),
              todayDecoration: BoxDecoration(
                color: Colors.blue.shade200,
                shape: BoxShape.circle,
              ),
              selectedDecoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary,
                shape: BoxShape.circle,
              ),
              weekendTextStyle: const TextStyle(color: Colors.red),
            ),

            // Style de l'en-tête
            headerStyle: const HeaderStyle(
              formatButtonVisible: true,
              titleCentered: true,
              formatButtonShowsNext: false,
            ),

            // Callbacks
            onDaySelected: (selectedDay, focusedDay) {
              setState(() {
                _selectedDay = selectedDay;
                _focusedDay = focusedDay;
              });
            },
            onFormatChanged: (format) {
              setState(() => _calendarFormat = format);
            },
            onPageChanged: (focusedDay) {
              _focusedDay = focusedDay;
              _loadEventsForMonth(focusedDay);
            },
          ),

          const SizedBox(height: 8),
          const Divider(),

          // Liste des événements du jour sélectionné
          Expanded(
            child: _buildEventsList(),
          ),
        ],
      ),
    );
  }

  /// Construit la liste des événements du jour sélectionné
  Widget _buildEventsList() {
    if (_selectedDay == null) {
      return const Center(child: Text('Sélectionnez un jour'));
    }

    final eventsForDay = _getEventsForDay(_selectedDay!);

    if (eventsForDay.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.event_busy, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Aucun événement',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              DateFormat('EEEE d MMMM yyyy', 'fr_FR').format(_selectedDay!),
              style: TextStyle(color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: eventsForDay.length,
      itemBuilder: (context, index) {
        final event = eventsForDay[index];
        final timeStr = event.start != null
            ? DateFormat('HH:mm').format(event.start!.toLocal())
            : 'Toute la journée';

        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          elevation: 2,
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: event.color.withOpacity(0.2),
              child: Icon(
                event.source == 'Google' ? Icons.calendar_today : Icons.calendar_month,
                color: event.color,
                size: 20,
              ),
            ),
            title: Text(
              event.title,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 16, color: Colors.grey[600]),
                    const SizedBox(width: 4),
                    Text(timeStr),
                  ],
                ),
                if (event.location != null && event.location!.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Icon(Icons.place, size: 16, color: Colors.grey[600]),
                      const SizedBox(width: 4),
                      Expanded(child: Text(event.location!)),
                    ],
                  ),
                ],
                const SizedBox(height: 2),
                Row(
                  children: [
                    Icon(
                      event.source == 'Google' ? Icons.g_mobiledata : Icons.business,
                      size: 16,
                      color: Colors.grey[600],
                    ),
                    const SizedBox(width: 4),
                    Text(
                      event.source,
                      style: TextStyle(
                        color: event.color,
                        fontWeight: FontWeight.w500,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            isThreeLine: true,
          ),
        );
      },
    );
  }
}

/// Modèle d'événement unifié pour Google et Microsoft
class CalendarEvent {
  final String title;
  final DateTime? start;
  final DateTime? end;
  final String? location;
  final String source; // 'Google' ou 'Microsoft'
  final Color color;

  CalendarEvent({
    required this.title,
    this.start,
    this.end,
    this.location,
    required this.source,
    required this.color,
  });

  @override
  String toString() => title;
}
