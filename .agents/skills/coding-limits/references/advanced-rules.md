# Coding Limits — Advanced Rules Reference

## very_good_analysis Setup

```yaml
# pubspec.yaml — dev_dependencies
dev_dependencies:
  very_good_analysis: ^6.0.0
```

```yaml
# analysis_options.yaml
include: package:very_good_analysis/analysis_options.yaml
```

Key rules enforced beyond `flutter_lints`:

| Rule | Meaning |
|------|---------|
| `avoid_print` | Use `developer.log()` instead |
| `prefer_const_constructors` | Add `const` to all eligible widgets |
| `always_use_package_imports` | No relative imports (use `package:iamhere/...`) |
| `public_member_api_docs` | Public APIs need doc comments |

> After enabling, run `flutter analyze` and fix all warnings before committing.

---

## Riverpod 3 — Bad Patterns to Avoid

```dart
// ❌ Riverpod 1/2 style — deprecated
final counterProvider = StateNotifierProvider<CounterNotifier, int>((ref) {
  return CounterNotifier();
});

// ❌ provider package — absolutely forbidden
final model = ChangeNotifierProvider((_) => MyModel());

// ✅ Riverpod 3 with code generation
@riverpod
class CounterNotifier extends _$CounterNotifier {
  @override
  int build() => 0;
  void increment() => state++;
}
```

---

## Ternary / Switch — Why It's Forbidden

Ternary and `switch` embed branching in expressions, making polymorphism impossible and unit testing harder. Replace with:

1. **Guard clauses** (early return)
2. **Sealed classes** with polymorphic dispatch
3. **Strategy pattern** (separate classes per case)

```dart
// ✅ guard clause
Future<Result<String?>> _doKakaoLogin() async {
  if (await isKakaoTalkInstalled()) return _loginWithApp();
  return _loginWithWeb();
}

// ❌ ternary
final result = isInstalled ? loginWithApp() : loginWithWeb();

// ❌ switch
switch (state) {
  case AuthState.loggedIn: return HomeRoute();
  case AuthState.loggedOut: return LoginRoute();
}
```
