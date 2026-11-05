import 'package:flutter/material.dart';

/// Tactile Button with interactive scale micro-animation on tap
class TactileBounceButton extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final double shrinkScale;
  final Duration duration;

  const TactileBounceButton({
    super.key,
    required this.child,
    this.onTap,
    this.shrinkScale = 0.94,
    this.duration = const Duration(milliseconds: 100),
  });

  @override
  State<TactileBounceButton> createState() => _TactileBounceButtonState();
}

class _TactileBounceButtonState extends State<TactileBounceButton>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: widget.duration,
      reverseDuration: const Duration(milliseconds: 150),
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: widget.shrinkScale,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
      reverseCurve: Curves.easeOutBack,
    ));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onPointerDown(PointerDownEvent event) {
    _controller.forward();
  }

  void _onPointerUp(PointerUpEvent event) {
    _controller.reverse();
  }

  void _onPointerCancel(PointerCancelEvent event) {
    _controller.reverse();
  }

  @override
  Widget build(BuildContext context) {
    Widget core = AnimatedBuilder(
      animation: _scaleAnimation,
      builder: (context, child) => Transform.scale(
        scale: _scaleAnimation.value,
        child: child,
      ),
      child: widget.child,
    );

    if (widget.onTap != null) {
      core = GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: widget.onTap,
        child: core,
      );
    }

    return Listener(
      onPointerDown: _onPointerDown,
      onPointerUp: _onPointerUp,
      onPointerCancel: _onPointerCancel,
      child: core,
    );
  }
}

/// Helper to display a pop-in dialog with spring scale and fade entrance
Future<T?> showPopInDialog<T>({
  required BuildContext context,
  required WidgetBuilder builder,
  bool barrierDismissible = true,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: barrierDismissible,
    barrierLabel: MaterialLocalizations.of(context).modalBarrierDismissLabel,
    barrierColor: Colors.black54,
    transitionDuration: const Duration(milliseconds: 280),
    pageBuilder: (ctx, anim1, anim2) => builder(ctx),
    transitionBuilder: (ctx, anim1, anim2, child) {
      final curved = CurvedAnimation(
        parent: anim1,
        curve: Curves.easeOutBack,
      );
      return ScaleTransition(
        scale: Tween<double>(begin: 0.8, end: 1.0).animate(curved),
        child: FadeTransition(
          opacity: anim1,
          child: child,
        ),
      );
    },
  );
}

/// Animated celebration dialog shown upon successful transaction submission
class TransactionCelebrationDialog extends StatefulWidget {
  final String title;
  final String voucherNumber;
  final String formattedAmount;
  final String accountName;
  final String? partyName;
  final bool isIncome;
  final VoidCallback onDone;
  final VoidCallback? onAddAnother;

  const TransactionCelebrationDialog({
    super.key,
    required this.title,
    required this.voucherNumber,
    required this.formattedAmount,
    required this.accountName,
    this.partyName,
    required this.isIncome,
    required this.onDone,
    this.onAddAnother,
  });

  @override
  State<TransactionCelebrationDialog> createState() => _TransactionCelebrationDialogState();
}

class _TransactionCelebrationDialogState extends State<TransactionCelebrationDialog>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _checkScale;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 400),
    );
    _checkScale = CurvedAnimation(
      parent: _controller,
      curve: Curves.elasticOut,
    );
    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isBW = theme.brightness == Brightness.light && theme.colorScheme.primary == const Color(0xFF000000);
    final accentColor = isBW
        ? const Color(0xFF000000)
        : (widget.isIncome ? Colors.green[700]! : Colors.red[700]!);

    return Dialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: isBW ? const BorderSide(color: Colors.black, width: 2) : BorderSide.none,
      ),
      elevation: isBW ? 0 : 6,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Pop-in animated check circle badge
            ScaleTransition(
              scale: _checkScale,
              child: Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isBW ? Colors.black : accentColor.withValues(alpha: 0.12),
                  border: Border.all(color: accentColor, width: 3),
                ),
                child: Icon(Icons.check, size: 44, color: isBW ? Colors.white : accentColor),
              ),
            ),
            const SizedBox(height: 16),

            // Title
            Text(
              widget.title,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 6),

            // Voucher Number Chip
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: isBW ? Colors.grey[200] : theme.colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(16),
                border: isBW ? Border.all(color: Colors.black, width: 1) : null,
              ),
              child: Text(
                'Voucher: ${widget.voucherNumber}',
                style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12),
              ),
            ),
            const SizedBox(height: 16),

            // Amount Display
            Text(
              widget.formattedAmount,
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                color: accentColor,
              ),
            ),
            const SizedBox(height: 12),

            // Details Box
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: isBW ? Colors.white : theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: isBW ? Colors.black : (theme.dividerTheme.color ?? Colors.grey.shade300)),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Account:', style: TextStyle(fontSize: 12, color: Colors.grey)),
                      Text(widget.accountName, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                    ],
                  ),
                  if (widget.partyName != null) ...[
                    const SizedBox(height: 6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Party Ledger:', style: TextStyle(fontSize: 12, color: Colors.grey)),
                        Text(widget.partyName!, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Buttons
            Row(
              children: [
                if (widget.onAddAnother != null)
                  Expanded(
                    child: OutlinedButton(
                      onPressed: widget.onAddAnother,
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      child: const Text('Add Another'),
                    ),
                  ),
                if (widget.onAddAnother != null) const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton(
                    onPressed: widget.onDone,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: accentColor,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    child: const Text('View Ledgers / Done', style: TextStyle(fontWeight: FontWeight.bold)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
