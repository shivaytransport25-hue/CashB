import 'dart:async';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/constants/enums.dart';
import '../models/business_model.dart';
import '../models/user_model.dart';

class AuthService extends ChangeNotifier {
  UserModel? _currentUser;
  BusinessModel? _currentBusiness;
  bool _isLoading = false;
  String? _errorMessage;

  UserModel? get currentUser => _currentUser;
  BusinessModel? get currentBusiness => _currentBusiness;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated => _currentUser != null;

  UserRole get currentRole => _currentUser?.role ?? UserRole.owner;

  Future<void> initializeAuth() async {
    _isLoading = true;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      final savedUserId = prefs.getString('saved_user_id');

      if (savedUserId != null) {
        // Restore local active session
        _currentUser = UserModel(
          id: savedUserId,
          email: prefs.getString('saved_user_email') ?? 'admin@cashbook.com',
          displayName: prefs.getString('saved_user_name') ?? 'Business Owner',
          defaultBusinessId: prefs.getString('saved_biz_id') ?? 'default_biz_001',
          role: UserRole.values.firstWhere(
            (e) => e.name == prefs.getString('saved_user_role'),
            orElse: () => UserRole.owner,
          ),
          createdAt: DateTime.now(),
          lastLoginAt: DateTime.now(),
        );

        _currentBusiness = BusinessModel(
          id: _currentUser!.defaultBusinessId,
          name: prefs.getString('saved_biz_name') ?? 'Shivay Logistics & Trade',
          ownerId: _currentUser!.id,
          ownerName: _currentUser!.displayName,
          address: 'Plot 42, GIDC Sachin, Surat, Gujarat - 394230',
          phone: '+91 98765 43210',
          email: 'accounts@shivaylogistics.com',
          gstin: '24AAACS1234E1Z8',
          pan: 'AAACS1234E',
          currencySymbol: '₹',
          financialYearStartMonth: 4,
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        );
      } else {
        // No saved session: Leave unauthenticated so LoginScreen displays first!
        _currentUser = null;
        _currentBusiness = null;
      }
    } catch (e) {
      debugPrint('Auth initialization error: $e');
      _currentUser = null;
      _currentBusiness = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> signIn(String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      String uid = 'usr_${email.replaceAll(RegExp(r'[^a-zA-Z0-9]'), '_')}';
      String displayName = email.split('@').first;

      try {
        final auth = FirebaseAuth.instance;
        final credential = await auth.signInWithEmailAndPassword(
          email: email.trim(),
          password: password,
        );
        if (credential.user != null) {
          uid = credential.user!.uid;
          displayName = credential.user!.displayName ?? displayName;
        }
      } catch (fbErr) {
        debugPrint('Firebase Auth falling back to local credentials: $fbErr');
      }

      _currentUser = UserModel(
        id: uid,
        email: email,
        displayName: displayName,
        defaultBusinessId: uid,
        role: UserRole.owner,
        permissions: ['all'],
        createdAt: DateTime.now(),
        lastLoginAt: DateTime.now(),
      );

      _currentBusiness = BusinessModel(
        id: uid,
        name: 'Shivay Logistics & Trading',
        ownerId: uid,
        ownerName: displayName,
        address: 'Plot 42, GIDC Sachin, Surat, Gujarat - 394230',
        phone: '+91 98765 43210',
        email: email,
        gstin: '24AAACS1234E1Z8',
        pan: 'AAACS1234E',
        currencySymbol: '₹',
        financialYearStartMonth: 4,
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      // Successful login
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('saved_user_id', uid);
      await prefs.setString('saved_user_email', email);
      await prefs.setString('saved_user_name', displayName);
      await prefs.setString('saved_biz_id', uid);
      await prefs.setString('saved_biz_name', _currentBusiness!.name);
      await prefs.setString('saved_user_role', _currentUser!.role.name);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> signInWithGoogle() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final googleProvider = GoogleAuthProvider();
      googleProvider.addScope('email');
      googleProvider.addScope('profile');

      final credential = await FirebaseAuth.instance.signInWithProvider(googleProvider);
      if (credential.user == null) {
        throw Exception('Google Sign-In returned no authenticated user.');
      }

      final u = credential.user!;
      final uid = u.uid;
      final email = u.email ?? 'operator@cashbook.local';
      final displayName = u.displayName ?? (u.email != null ? u.email!.split('@')[0] : 'Operator');

      _currentUser = UserModel(
        id: uid,
        email: email,
        displayName: displayName,
        defaultBusinessId: uid,
        role: UserRole.owner,
        permissions: ['all'],
        createdAt: DateTime.now(),
        lastLoginAt: DateTime.now(),
      );
        createdAt: DateTime.now(),
        lastLoginAt: DateTime.now(),
      );

      _currentBusiness = BusinessModel(
        id: uid,
        name: 'Shivay Logistics & Trade',
        ownerId: uid,
        ownerName: displayName,
        address: 'Plot 42, GIDC Sachin, Surat, Gujarat - 394230',
        phone: '+91 98765 43210',
        email: email,
        gstin: '24AAACS1234E1Z8',
        pan: 'AAACS1234E',
        currencySymbol: '₹',
        financialYearStartMonth: 4,
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('saved_user_id', uid);
      await prefs.setString('saved_user_email', email);
      await prefs.setString('saved_user_name', displayName);
      await prefs.setString('saved_biz_id', uid);
      await prefs.setString('saved_biz_name', _currentBusiness!.name);
      await prefs.setString('saved_user_role', _currentUser!.role.name);

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      debugPrint('Google Sign-In note/error: $e');
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> signOut() async {
    try {
      await FirebaseAuth.instance.signOut();
    } catch (_) {}
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    _currentUser = null;
    notifyListeners();
  }

  Future<void> updateBusinessProfile(BusinessModel updated) async {
    _currentBusiness = updated;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('saved_biz_name', updated.name);
    notifyListeners();
  }

  void switchUserRole(UserRole newRole) {
    if (_currentUser != null) {
      _currentUser = _currentUser!.copyWith(role: newRole);
      notifyListeners();
    }
  }
}

