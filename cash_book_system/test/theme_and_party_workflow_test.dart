import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:crypto/crypto.dart';

import 'package:shared_preferences/shared_preferences.dart';
import 'package:cash_book_system/core/constants/enums.dart';
import 'package:cash_book_system/core/animations/bounce_button.dart';
import 'package:cash_book_system/core/theme/theme_provider.dart';
import 'package:cash_book_system/models/party_model.dart';
import 'package:cash_book_system/services/backup_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  SharedPreferences.setMockInitialValues({});
  group('1. Theme Engine & Black and White Mode Tests', () {
    test('Default theme is Emerald Green in Light mode', () {
      final provider = ThemeProvider();
      expect(provider.colorTheme, AppColorTheme.emeraldGreen);
      expect(provider.appearanceMode, AppAppearanceMode.light);
      expect(provider.isBlackAndWhite, isFalse);
      expect(provider.isDarkMode, isFalse);
    });

    test('Color theme palette switching updates primary colors', () {
      final provider = ThemeProvider();

      provider.setColorTheme(AppColorTheme.royalBlue);
      expect(provider.colorTheme, AppColorTheme.royalBlue);
      expect(provider.themeData.colorScheme.primary, const Color(0xFF0D47A1));

      provider.setColorTheme(AppColorTheme.indigoPurple);
      expect(provider.colorTheme, AppColorTheme.indigoPurple);
      expect(provider.themeData.colorScheme.primary, const Color(0xFF4A148C));

      provider.setColorTheme(AppColorTheme.warmAmber);
      expect(provider.colorTheme, AppColorTheme.warmAmber);
      expect(provider.themeData.colorScheme.primary, const Color(0xFFE65100));

      provider.setColorTheme(AppColorTheme.crimsonRose);
      expect(provider.colorTheme, AppColorTheme.crimsonRose);
      expect(provider.themeData.colorScheme.primary, const Color(0xFF880E4F));
    });

    test('Dedicated Black & White mode provides high contrast monochrome theme', () {
      final provider = ThemeProvider();
      provider.setAppearanceMode(AppAppearanceMode.blackAndWhite);

      expect(provider.isBlackAndWhite, isTrue);
      final theme = provider.themeData;
      expect(theme.colorScheme.primary, const Color(0xFF000000));
      expect(theme.colorScheme.onPrimary, const Color(0xFFFFFFFF));
      expect(theme.scaffoldBackgroundColor, const Color(0xFFFFFFFF));
      expect(theme.cardTheme.color, Colors.white);
      expect(theme.cardTheme.shape, isA<RoundedRectangleBorder>());
    });

    test('Dark mode correctly updates brightness and dark colors', () {
      final provider = ThemeProvider();
      provider.setAppearanceMode(AppAppearanceMode.dark);

      expect(provider.isDarkMode, isTrue);
      expect(provider.themeData.brightness, Brightness.dark);
    });

    test('toggleTheme cycles smoothly between Light -> Dark -> System -> Black&White -> Light', () {
      final provider = ThemeProvider();
      expect(provider.appearanceMode, AppAppearanceMode.light);

      provider.toggleTheme();
      expect(provider.appearanceMode, AppAppearanceMode.dark);

      provider.toggleTheme();
      expect(provider.appearanceMode, AppAppearanceMode.system);

      provider.toggleTheme();
      expect(provider.appearanceMode, AppAppearanceMode.blackAndWhite);

      provider.toggleTheme();
      expect(provider.appearanceMode, AppAppearanceMode.light);
    });

    test('System mode resolves themeData based on platform brightness', () {
      final provider = ThemeProvider();
      provider.setAppearanceMode(AppAppearanceMode.system);
      expect(provider.appearanceMode, AppAppearanceMode.system);
      final theme = provider.themeData;
      expect(theme, isNotNull);
    });
  });

  group('2. Party Autocomplete & Filter Logic Tests', () {
    final now = DateTime.now();
    final parties = [
      PartyModel(
        id: 'p1',
        businessId: 'biz_test',
        name: 'Reliance Logistics Client',
        partyType: PartyType.customer,
        phone: '+91 98980 11223',
        address: 'Hazira Complex, Surat',
        openingBalancePaise: 4500000,
        currentBalancePaise: 4500000,
        createdAt: now,
        updatedAt: now,
      ),
      PartyModel(
        id: 'p2',
        businessId: 'biz_test',
        name: 'Indian Oil Depot',
        partyType: PartyType.supplier,
        phone: '+91 94260 44556',
        address: 'Magdalla Port Road',
        openingBalancePaise: 3000000,
        currentBalancePaise: -3000000,
        createdAt: now,
        updatedAt: now,
      ),
      PartyModel(
        id: 'p3',
        businessId: 'biz_test',
        name: 'Mukeshbhai Truck Driver',
        partyType: PartyType.employee,
        phone: '+91 97250 88990',
        currentBalancePaise: 0,
        createdAt: now,
        updatedAt: now,
      ),
      PartyModel(
        id: 'p4',
        businessId: 'biz_test',
        name: 'Ramesh Patel Transport',
        partyType: PartyType.customer,
        phone: '+91 99000 12345',
        currentBalancePaise: 150000,
        createdAt: now,
        updatedAt: now,
      ),
    ];

    List<PartyModel> filterParties(String query) {
      final q = query.trim().toLowerCase();
      if (q.isEmpty) return parties;
      return parties.where((p) {
        final nameMatch = p.name.toLowerCase().contains(q);
        final phoneMatch = p.phone?.toLowerCase().contains(q) ?? false;
        final addrMatch = p.address?.toLowerCase().contains(q) ?? false;
        return nameMatch || phoneMatch || addrMatch;
      }).toList();
    }

    test('Matches parties by case-insensitive name substring', () {
      final result1 = filterParties('reliance');
      expect(result1.length, 1);
      expect(result1.first.id, 'p1');

      final result2 = filterParties('RAMESH');
      expect(result2.length, 1);
      expect(result2.first.id, 'p4');

      final result3 = filterParties('truck');
      expect(result3.length, 1);
      expect(result3.first.id, 'p3');
    });

    test('Matches parties by phone number substring', () {
      final result = filterParties('94260');
      expect(result.length, 1);
      expect(result.first.id, 'p2');
    });

    test('Returns all parties when search query is empty', () {
      final result = filterParties('');
      expect(result.length, 4);
    });

    test('Returns empty list when party does not exist (triggers inline party creation prompt)', () {
      final result = filterParties('Ganesh Steels');
      expect(result, isEmpty);
    });

    test('Exact name matching links party even when typed without selecting from dropdown', () {
      final query = '  reliance logistics client  ';
      PartyModel? matchedParty;
      for (final p in parties) {
        if (p.name.trim().toLowerCase() == query.trim().toLowerCase()) {
          matchedParty = p;
          break;
        }
      }

      expect(matchedParty, isNotNull);
      expect(matchedParty!.id, 'p1');
    });

    test('Unicode and special character / emoji search in party autocomplete', () {
      final unicodeParty = PartyModel(
        id: 'p_unicode',
        businessId: 'biz_test',
        name: 'Om Sai Enterprise 🚛 (શ્રી ગણેશ)',
        partyType: PartyType.supplier,
        phone: '+91 99999 88888',
        address: 'Bhavnagar - 364001',
        createdAt: now,
        updatedAt: now,
      );

      final combined = [...parties, unicodeParty];

      final res1 = combined.where((p) => p.name.toLowerCase().contains('🚛')).toList();
      expect(res1.length, 1);
      expect(res1.first.id, 'p_unicode');

      final res2 = combined.where((p) => p.name.toLowerCase().contains('શ્રી ગણેશ')).toList();
      expect(res2.length, 1);
      expect(res2.first.id, 'p_unicode');

      final res3 = combined.where((p) => p.name.toLowerCase().contains('sai')).toList();
      expect(res3.length, 1);
      expect(res3.first.id, 'p_unicode');
    });
  });

  group('3. Tactile Button Micro-Animation & Dialog Tests', () {
    testWidgets('TactileBounceButton renders child correctly and handles tap', (tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Center(
              child: TactileBounceButton(
                onTap: () => tapped = true,
                child: const Text('Test Tap Button'),
              ),
            ),
          ),
        ),
      );

      expect(find.text('Test Tap Button'), findsOneWidget);

      await tester.tap(find.text('Test Tap Button'));
      await tester.pumpAndSettle();

      expect(tapped, isTrue);
    });
  });

  group('4. Cloud Vault Cryptographic Backup Verification Tests', () {
    test('Valid backup passes SHA-256 integrity check', () {
      final transactions = [
        {'id': 'txn_1', 'amountPaise': 500000, 'voucher': 'VCH-001'},
        {'id': 'txn_2', 'amountPaise': 150000, 'voucher': 'VCH-002'},
      ];
      final serialized = jsonEncode(transactions);
      final digest = sha256.convert(utf8.encode(serialized)).toString();

      final backupPackage = {
        'version': '1.0',
        'businessId': 'biz_test_001',
        'transactions': transactions,
        'checksumSha256': digest,
      };

      final isValid = BackupService.verifyBackupIntegrity(backupPackage);
      expect(isValid, isTrue);
    });

    test('Tampered backup fails SHA-256 integrity check', () {
      final transactions = [
        {'id': 'txn_1', 'amountPaise': 500000, 'voucher': 'VCH-001'},
      ];
      final serialized = jsonEncode(transactions);
      final digest = sha256.convert(utf8.encode(serialized)).toString();

      final tamperedPackage = {
        'version': '1.0',
        'businessId': 'biz_test_001',
        // Hacker altered amount from 500000 to 99999999
        'transactions': [
          {'id': 'txn_1', 'amountPaise': 99999999, 'voucher': 'VCH-001'},
        ],
        'checksumSha256': digest,
      };

      final isValid = BackupService.verifyBackupIntegrity(tamperedPackage);
      expect(isValid, isFalse);
    });
  });
}
