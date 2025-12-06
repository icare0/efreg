import 'package:google_sign_in/google_sign_in.dart';
import 'package:googleapis/calendar/v3.dart' as calendar;
import 'package:googleapis_auth/googleapis_auth.dart' as auth;
import 'package:http/http.dart' as http;

/// Service pour l'intégration Google Calendar
/// Gère l'authentification OAuth2 et la récupération des événements
class GoogleCalendarService {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: [
      calendar.CalendarApi.calendarReadonlyScope,
    ],
  );

  GoogleSignInAccount? _currentUser;
  calendar.CalendarApi? _calendarApi;

  /// Retourne l'utilisateur actuellement connecté
  GoogleSignInAccount? get currentUser => _currentUser;

  /// Vérifie si l'utilisateur est connecté
  bool get isSignedIn => _currentUser != null;

  /// Connecte l'utilisateur via Google Sign-In
  /// Retourne true si la connexion réussit, false sinon
  Future<bool> signIn() async {
    try {
      final account = await _googleSignIn.signIn();
      if (account == null) {
        return false; // L'utilisateur a annulé
      }

      _currentUser = account;
      await _initCalendarApi();
      return true;
    } catch (error) {
      print('Erreur lors de la connexion Google: $error');
      return false;
    }
  }

  /// Déconnecte l'utilisateur Google
  Future<void> signOut() async {
    await _googleSignIn.signOut();
    _currentUser = null;
    _calendarApi = null;
  }

  /// Initialise l'API Google Calendar avec les credentials de l'utilisateur
  Future<void> _initCalendarApi() async {
    if (_currentUser == null) return;

    final authHeaders = await _currentUser!.authHeaders;
    final authenticateClient = _AuthenticatedClient(authHeaders);
    _calendarApi = calendar.CalendarApi(authenticateClient);
  }

  /// Récupère les événements du jour depuis Google Calendar
  /// Retourne une liste d'événements ou null en cas d'erreur
  Future<List<calendar.Event>?> getTodayEvents() async {
    if (_calendarApi == null) {
      print('Calendar API non initialisée');
      return null;
    }

    try {
      final now = DateTime.now();
      final startOfDay = DateTime(now.year, now.month, now.day);
      final endOfDay = DateTime(now.year, now.month, now.day, 23, 59, 59);

      final events = await _calendarApi!.events.list(
        'primary',
        timeMin: startOfDay.toUtc(),
        timeMax: endOfDay.toUtc(),
        singleEvents: true,
        orderBy: 'startTime',
      );

      return events.items ?? [];
    } catch (error) {
      print('Erreur lors de la récupération des événements: $error');
      return null;
    }
  }

  /// Récupère les événements entre deux dates
  Future<List<calendar.Event>?> getEventsBetween(
    DateTime start,
    DateTime end,
  ) async {
    if (_calendarApi == null) {
      print('Calendar API non initialisée');
      return null;
    }

    try {
      final events = await _calendarApi!.events.list(
        'primary',
        timeMin: start.toUtc(),
        timeMax: end.toUtc(),
        singleEvents: true,
        orderBy: 'startTime',
      );

      return events.items ?? [];
    } catch (error) {
      print('Erreur lors de la récupération des événements: $error');
      return null;
    }
  }
}

/// Client HTTP authentifié pour les requêtes Google API
class _AuthenticatedClient extends http.BaseClient {
  final Map<String, String> _headers;
  final http.Client _client = http.Client();

  _AuthenticatedClient(this._headers);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    request.headers.addAll(_headers);
    return _client.send(request);
  }

  @override
  void close() {
    _client.close();
    super.close();
  }
}
