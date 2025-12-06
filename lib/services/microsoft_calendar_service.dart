import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

/// Modèle pour un événement Microsoft Calendar
class MicrosoftCalendarEvent {
  final String id;
  final String subject;
  final DateTime? start;
  final DateTime? end;
  final String? location;

  MicrosoftCalendarEvent({
    required this.id,
    required this.subject,
    this.start,
    this.end,
    this.location,
  });

  factory MicrosoftCalendarEvent.fromJson(Map<String, dynamic> json) {
    DateTime? parseDateTime(Map<String, dynamic>? dateTimeData) {
      if (dateTimeData == null || dateTimeData['dateTime'] == null) {
        return null;
      }
      return DateTime.parse(dateTimeData['dateTime'] as String);
    }

    return MicrosoftCalendarEvent(
      id: json['id'] as String,
      subject: json['subject'] as String? ?? 'Sans titre',
      start: parseDateTime(json['start'] as Map<String, dynamic>?),
      end: parseDateTime(json['end'] as Map<String, dynamic>?),
      location: json['location']?['displayName'] as String?,
    );
  }
}

/// Service pour l'intégration Microsoft Outlook Calendar
/// Gère l'authentification OAuth2 et la récupération des événements via Microsoft Graph API
class MicrosoftCalendarService {
  // Configuration OAuth2 Microsoft
  // IMPORTANT: Remplacez ces valeurs par vos propres credentials
  // Obtenez-les sur https://portal.azure.com
  static const String _clientId = 'YOUR_MICROSOFT_CLIENT_ID';
  static const String _tenantId = 'common'; // ou votre tenant ID spécifique
  static const String _redirectUri = 'msauth://flutter_gps_calendar_poc/auth';
  static const String _scope = 'User.Read Calendars.Read offline_access';

  String? _accessToken;
  String? _userName;

  /// Retourne l'utilisateur actuellement connecté
  String? get currentUser => _userName;

  /// Vérifie si l'utilisateur est connecté
  bool get isSignedIn => _accessToken != null;

  /// Connecte l'utilisateur via Microsoft OAuth2
  /// Retourne true si la connexion réussit, false sinon
  Future<bool> signIn() async {
    try {
      final authUrl = Uri.https('login.microsoftonline.com', '/$_tenantId/oauth2/v2.0/authorize', {
        'client_id': _clientId,
        'response_type': 'code',
        'redirect_uri': _redirectUri,
        'response_mode': 'query',
        'scope': _scope,
      });

      // Ouvre le navigateur pour l'authentification
      final result = await FlutterWebAuth2.authenticate(
        url: authUrl.toString(),
        callbackUrlScheme: 'msauth',
      );

      // Extrait le code d'autorisation
      final code = Uri.parse(result).queryParameters['code'];
      if (code == null) {
        return false;
      }

      // Échange le code contre un access token
      final tokenResponse = await http.post(
        Uri.https('login.microsoftonline.com', '/$_tenantId/oauth2/v2.0/token'),
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: {
          'client_id': _clientId,
          'scope': _scope,
          'code': code,
          'redirect_uri': _redirectUri,
          'grant_type': 'authorization_code',
        },
      );

      if (tokenResponse.statusCode != 200) {
        print('Erreur lors de l\'obtention du token: ${tokenResponse.body}');
        return false;
      }

      final tokenData = json.decode(tokenResponse.body);
      _accessToken = tokenData['access_token'] as String?;

      // Récupère les informations de l'utilisateur
      await _fetchUserInfo();

      return true;
    } catch (error) {
      print('Erreur lors de la connexion Microsoft: $error');
      return false;
    }
  }

  /// Récupère les informations de l'utilisateur connecté
  Future<void> _fetchUserInfo() async {
    if (_accessToken == null) return;

    try {
      final response = await http.get(
        Uri.https('graph.microsoft.com', '/v1.0/me'),
        headers: {'Authorization': 'Bearer $_accessToken'},
      );

      if (response.statusCode == 200) {
        final userData = json.decode(response.body);
        _userName = userData['displayName'] as String?;
      }
    } catch (error) {
      print('Erreur lors de la récupération des infos utilisateur: $error');
    }
  }

  /// Déconnecte l'utilisateur Microsoft
  Future<void> signOut() async {
    _accessToken = null;
    _userName = null;
  }

  /// Récupère les événements du jour depuis Microsoft Calendar
  /// Retourne une liste d'événements ou null en cas d'erreur
  Future<List<MicrosoftCalendarEvent>?> getTodayEvents() async {
    if (_accessToken == null) {
      print('Utilisateur non connecté');
      return null;
    }

    try {
      final now = DateTime.now();
      final startOfDay = DateTime(now.year, now.month, now.day);
      final endOfDay = DateTime(now.year, now.month, now.day, 23, 59, 59);

      return await getEventsBetween(startOfDay, endOfDay);
    } catch (error) {
      print('Erreur lors de la récupération des événements: $error');
      return null;
    }
  }

  /// Récupère les événements entre deux dates via Microsoft Graph API
  Future<List<MicrosoftCalendarEvent>?> getEventsBetween(
    DateTime start,
    DateTime end,
  ) async {
    if (_accessToken == null) {
      print('Utilisateur non connecté');
      return null;
    }

    try {
      final startStr = start.toUtc().toIso8601String();
      final endStr = end.toUtc().toIso8601String();

      final response = await http.get(
        Uri.https(
          'graph.microsoft.com',
          '/v1.0/me/calendarview',
          {
            'startdatetime': startStr,
            'enddatetime': endStr,
            '\$orderby': 'start/dateTime',
          },
        ),
        headers: {
          'Authorization': 'Bearer $_accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode != 200) {
        print('Erreur API: ${response.statusCode} - ${response.body}');
        return null;
      }

      final data = json.decode(response.body);
      final events = (data['value'] as List)
          .map((e) => MicrosoftCalendarEvent.fromJson(e as Map<String, dynamic>))
          .toList();

      return events;
    } catch (error) {
      print('Erreur lors de la récupération des événements: $error');
      return null;
    }
  }
}
