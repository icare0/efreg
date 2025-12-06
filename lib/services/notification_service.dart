import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';

/// Service de notifications locales
/// Gère l'envoi de notifications push locales sur l'appareil
class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  bool _isInitialized = false;

  /// Initialise le service de notifications
  /// Doit être appelé au démarrage de l'app
  Future<void> initialize() async {
    if (_isInitialized) return;

    // Configuration Android
    const AndroidInitializationSettings androidSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    // Configuration iOS
    const DarwinInitializationSettings iosSettings =
        DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    // Configuration globale
    const InitializationSettings settings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    // Initialiser le plugin
    await _notificationsPlugin.initialize(
      settings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    _isInitialized = true;
  }

  /// Callback quand l'utilisateur tape sur une notification
  void _onNotificationTapped(NotificationResponse response) {
    print('Notification tappée : ${response.payload}');
  }

  /// Demande les permissions de notifications (Android 13+ et iOS)
  Future<bool> requestPermissions() async {
    if (!_isInitialized) {
      await initialize();
    }

    // Demander la permission de notification (Android 13+ et iOS)
    final status = await Permission.notification.request();

    if (status.isGranted) {
      print('✅ Permission de notification accordée');
      return true;
    } else if (status.isDenied) {
      print('❌ Permission de notification refusée');
      return false;
    } else if (status.isPermanentlyDenied) {
      print('❌ Permission de notification refusée définitivement');
      // Ouvrir les paramètres de l'app
      await openAppSettings();
      return false;
    }

    return false;
  }

  /// Envoie une notification simple de test
  Future<void> showTestNotification() async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'test_channel',
      'Notifications de test',
      channelDescription: 'Canal pour les notifications de test',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      0,
      'Test de notification',
      'Ceci est une notification de test du POC Flutter !',
      details,
      payload: 'test_notification',
    );
  }

  /// Envoie une notification avec position GPS
  Future<void> showGPSNotification(double lat, double lng) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'gps_channel',
      'Notifications GPS',
      channelDescription: 'Notifications liées à la géolocalisation',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      1,
      'Position GPS',
      'Lat: ${lat.toStringAsFixed(4)}, Lng: ${lng.toStringAsFixed(4)}',
      details,
      payload: 'gps_notification',
    );
  }

  /// Envoie une notification pour un événement calendrier
  Future<void> showCalendarNotification(String eventTitle, String provider) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'calendar_channel',
      'Notifications Calendrier',
      channelDescription: 'Notifications pour les événements du calendrier',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      2,
      'Événement $provider',
      eventTitle,
      details,
      payload: 'calendar_notification',
    );
  }

  /// Envoie une notification planifiée (dans X secondes)
  Future<void> showScheduledNotification(int delaySeconds) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'scheduled_channel',
      'Notifications planifiées',
      channelDescription: 'Notifications planifiées dans le futur',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    // Note: Pour les notifications planifiées, il faut utiliser le package timezone
    // Pour ce POC, on envoie juste une notification immédiate
    await Future.delayed(Duration(seconds: delaySeconds));

    await _notificationsPlugin.show(
      3,
      'Notification planifiée',
      'Cette notification était planifiée il y a $delaySeconds secondes !',
      details,
      payload: 'scheduled_notification',
    );
  }

  /// Annule toutes les notifications
  Future<void> cancelAllNotifications() async {
    await _notificationsPlugin.cancelAll();
  }

  /// Annule une notification spécifique
  Future<void> cancelNotification(int id) async {
    await _notificationsPlugin.cancel(id);
  }
}
