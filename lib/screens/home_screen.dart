import 'dart:async';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:googleapis/calendar/v3.dart' as google_calendar;
import '../services/location_service.dart';
import '../services/google_calendar_service.dart';
import '../services/microsoft_calendar_service.dart';

/// Écran principal du POC
/// Regroupe : login Google/Microsoft, position GPS en temps réel, événements du jour
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  // Services
  final LocationService _locationService = LocationService();
  final GoogleCalendarService _googleCalendarService = GoogleCalendarService();
  final MicrosoftCalendarService _microsoftCalendarService = MicrosoftCalendarService();

  // État GPS
  Position? _currentPosition;
  StreamSubscription<Position>? _positionStreamSubscription;
  String _gpsStatus = 'Non démarré';

  // État Google Calendar
  bool _isGoogleSignedIn = false;
  List<google_calendar.Event>? _googleEvents;
  bool _isLoadingGoogleEvents = false;

  // État Microsoft Calendar
  bool _isMicrosoftSignedIn = false;
  List<MicrosoftCalendarEvent>? _microsoftEvents;
  bool _isLoadingMicrosoftEvents = false;

  @override
  void initState() {
    super.initState();
    _startLocationTracking();
  }

  @override
  void dispose() {
    _positionStreamSubscription?.cancel();
    super.dispose();
  }

  /// Démarre le suivi GPS en temps réel
  Future<void> _startLocationTracking() async {
    try {
      setState(() => _gpsStatus = 'Vérification des permissions...');

      final hasPermission = await _locationService.checkAndRequestPermissions();
      if (!hasPermission) {
        setState(() => _gpsStatus = 'Permissions refusées');
        return;
      }

      setState(() => _gpsStatus = 'Suivi GPS actif');

      // Écoute les changements de position
      _positionStreamSubscription = _locationService.getPositionStream().listen(
        (Position position) {
          setState(() => _currentPosition = position);
        },
        onError: (error) {
          setState(() => _gpsStatus = 'Erreur: $error');
        },
      );
    } catch (e) {
      setState(() => _gpsStatus = 'Erreur: $e');
    }
  }

  /// Connexion Google et chargement des événements
  Future<void> _handleGoogleSignIn() async {
    setState(() => _isLoadingGoogleEvents = true);

    final success = await _googleCalendarService.signIn();
    if (success) {
      setState(() => _isGoogleSignedIn = true);
      await _loadGoogleEvents();
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de la connexion Google')),
        );
      }
    }

    setState(() => _isLoadingGoogleEvents = false);
  }

  /// Charge les événements Google du jour
  Future<void> _loadGoogleEvents() async {
    setState(() => _isLoadingGoogleEvents = true);

    final events = await _googleCalendarService.getTodayEvents();
    setState(() {
      _googleEvents = events;
      _isLoadingGoogleEvents = false;
    });
  }

  /// Déconnexion Google
  Future<void> _handleGoogleSignOut() async {
    await _googleCalendarService.signOut();
    setState(() {
      _isGoogleSignedIn = false;
      _googleEvents = null;
    });
  }

  /// Connexion Microsoft et chargement des événements
  Future<void> _handleMicrosoftSignIn() async {
    setState(() => _isLoadingMicrosoftEvents = true);

    final success = await _microsoftCalendarService.signIn();
    if (success) {
      setState(() => _isMicrosoftSignedIn = true);
      await _loadMicrosoftEvents();
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Échec de la connexion Microsoft')),
        );
      }
    }

    setState(() => _isLoadingMicrosoftEvents = false);
  }

  /// Charge les événements Microsoft du jour
  Future<void> _loadMicrosoftEvents() async {
    setState(() => _isLoadingMicrosoftEvents = true);

    final events = await _microsoftCalendarService.getTodayEvents();
    setState(() {
      _microsoftEvents = events;
      _isLoadingMicrosoftEvents = false;
    });
  }

  /// Déconnexion Microsoft
  Future<void> _handleMicrosoftSignOut() async {
    await _microsoftCalendarService.signOut();
    setState(() {
      _isMicrosoftSignedIn = false;
      _microsoftEvents = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('POC GPS & Calendar'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Section GPS
            _buildGpsSection(),
            const SizedBox(height: 24),
            const Divider(),
            const SizedBox(height: 24),

            // Section Google Calendar
            _buildGoogleCalendarSection(),
            const SizedBox(height: 24),
            const Divider(),
            const SizedBox(height: 24),

            // Section Microsoft Calendar
            _buildMicrosoftCalendarSection(),
          ],
        ),
      ),
    );
  }

  /// Widget pour la section GPS
  Widget _buildGpsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.location_on, color: Colors.blue, size: 28),
                const SizedBox(width: 8),
                Text(
                  'Position GPS',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text('Statut: $_gpsStatus'),
            if (_currentPosition != null) ...[
              const SizedBox(height: 8),
              Text('Latitude: ${_currentPosition!.latitude.toStringAsFixed(6)}'),
              Text('Longitude: ${_currentPosition!.longitude.toStringAsFixed(6)}'),
              Text('Précision: ${_currentPosition!.accuracy.toStringAsFixed(2)} m'),
              Text('Altitude: ${_currentPosition!.altitude.toStringAsFixed(1)} m'),
            ],
          ],
        ),
      ),
    );
  }

  /// Widget pour la section Google Calendar
  Widget _buildGoogleCalendarSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Image.asset(
                  'assets/google_logo.png',
                  width: 24,
                  height: 24,
                  errorBuilder: (_, __, ___) => const Icon(Icons.calendar_today, color: Colors.red),
                ),
                const SizedBox(width: 8),
                Text(
                  'Google Calendar',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (!_isGoogleSignedIn)
              ElevatedButton.icon(
                onPressed: _isLoadingGoogleEvents ? null : _handleGoogleSignIn,
                icon: const Icon(Icons.login),
                label: Text(_isLoadingGoogleEvents ? 'Connexion...' : 'Se connecter à Google'),
              )
            else ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Connecté: ${_googleCalendarService.currentUser?.displayName ?? "Utilisateur"}'),
                  TextButton.icon(
                    onPressed: _handleGoogleSignOut,
                    icon: const Icon(Icons.logout),
                    label: const Text('Déconnexion'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              ElevatedButton.icon(
                onPressed: _isLoadingGoogleEvents ? null : _loadGoogleEvents,
                icon: const Icon(Icons.refresh),
                label: const Text('Actualiser les événements'),
              ),
              const SizedBox(height: 12),
              if (_isLoadingGoogleEvents)
                const Center(child: CircularProgressIndicator())
              else if (_googleEvents == null)
                const Text('Aucun événement chargé')
              else if (_googleEvents!.isEmpty)
                const Text('Aucun événement aujourd\'hui')
              else
                _buildGoogleEventsList(),
            ],
          ],
        ),
      ),
    );
  }

  /// Widget pour afficher la liste des événements Google
  Widget _buildGoogleEventsList() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Événements du jour (${_googleEvents!.length}):',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        ..._googleEvents!.map((event) {
          final start = event.start?.dateTime ?? event.start?.date;
          final timeStr = start != null
              ? '${start.toLocal().hour}:${start.toLocal().minute.toString().padLeft(2, '0')}'
              : 'Toute la journée';

          return Card(
            margin: const EdgeInsets.symmetric(vertical: 4),
            child: ListTile(
              leading: const Icon(Icons.event, color: Colors.blue),
              title: Text(event.summary ?? 'Sans titre'),
              subtitle: Text(timeStr),
              trailing: event.location != null
                  ? const Icon(Icons.place, size: 16)
                  : null,
            ),
          );
        }),
      ],
    );
  }

  /// Widget pour la section Microsoft Calendar
  Widget _buildMicrosoftCalendarSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.calendar_month, color: Colors.blueAccent, size: 28),
                const SizedBox(width: 8),
                Text(
                  'Microsoft Outlook Calendar',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (!_isMicrosoftSignedIn)
              ElevatedButton.icon(
                onPressed: _isLoadingMicrosoftEvents ? null : _handleMicrosoftSignIn,
                icon: const Icon(Icons.login),
                label: Text(_isLoadingMicrosoftEvents ? 'Connexion...' : 'Se connecter à Microsoft'),
              )
            else ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Connecté: ${_microsoftCalendarService.currentUser ?? "Utilisateur"}'),
                  TextButton.icon(
                    onPressed: _handleMicrosoftSignOut,
                    icon: const Icon(Icons.logout),
                    label: const Text('Déconnexion'),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              ElevatedButton.icon(
                onPressed: _isLoadingMicrosoftEvents ? null : _loadMicrosoftEvents,
                icon: const Icon(Icons.refresh),
                label: const Text('Actualiser les événements'),
              ),
              const SizedBox(height: 12),
              if (_isLoadingMicrosoftEvents)
                const Center(child: CircularProgressIndicator())
              else if (_microsoftEvents == null)
                const Text('Aucun événement chargé')
              else if (_microsoftEvents!.isEmpty)
                const Text('Aucun événement aujourd\'hui')
              else
                _buildMicrosoftEventsList(),
            ],
          ],
        ),
      ),
    );
  }

  /// Widget pour afficher la liste des événements Microsoft
  Widget _buildMicrosoftEventsList() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Événements du jour (${_microsoftEvents!.length}):',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        ..._microsoftEvents!.map((event) {
          final timeStr = event.start != null
              ? '${event.start!.toLocal().hour}:${event.start!.toLocal().minute.toString().padLeft(2, '0')}'
              : 'Toute la journée';

          return Card(
            margin: const EdgeInsets.symmetric(vertical: 4),
            child: ListTile(
              leading: const Icon(Icons.event, color: Colors.blueAccent),
              title: Text(event.subject),
              subtitle: Text(timeStr),
              trailing: event.location != null
                  ? const Icon(Icons.place, size: 16)
                  : null,
            ),
          );
        }),
      ],
    );
  }
}
