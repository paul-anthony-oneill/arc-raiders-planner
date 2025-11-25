# Gemini CLI Custom Commands - ARC Raiders Planner

This directory contains custom commands and guidelines for the Gemini CLI to assist with UI/UX development and code quality for the ARC Raiders Loot Planner project.

## 📁 Structure

```
.gemini/
├── commands/                    # Custom Gemini CLI commands
│   ├── ux-ui-audit.toml        # UX/UI guideline audit
│   ├── component-review.toml   # React component code review
│   ├── performance-audit.toml  # Performance optimization audit
│   └── accessibility-check.toml # WCAG 2.1 AA compliance check
└── README.md                    # This file

guidelines/
└── ux-guidelines/
    ├── ui.md                    # UI design principles
    └── ux.md                    # UX design patterns
```

## 🚀 Available Commands

### 1. `/uxuiaudit` - UX/UI Design Audit

Audits a component against the project's established UI and UX guidelines.

**Usage:**
```bash
/uxuiaudit frontend/src/components/MapViewer.tsx
```

**What it checks:**
- Visual design compliance (spacing, typography, colors)
- Material UI v7 patterns and best practices
- Accessibility standards
- Responsive design implementation
- Gaming aesthetic and brand alignment
- User flow and interaction patterns
- Loading, error, and empty states

**Output includes:**
- Summary of findings
- Specific guideline violations with references
- Actionable recommendations with code examples
- Strengths and compliant areas

---

### 2. `/componentreview` - React Component Code Review

Comprehensive code review focusing on React 19 + TypeScript best practices.

**Usage:**
```bash
/componentreview frontend/src/components/ItemSearch.tsx
```

**What it checks:**
- React patterns (hooks, composition, Suspense)
- TypeScript type safety
- Performance considerations
- Material UI v7 usage
- Code quality and maintainability
- State management decisions
- Error handling

**Output includes:**
- Critical issues that must be fixed
- Improvement suggestions
- Best practices the component follows
- Before/after code examples

---

### 3. `/performanceaudit` - Performance Optimization Audit

Deep dive into React performance issues and optimization opportunities.

**Usage:**
```bash
/performanceaudit frontend/src/screens/PlannerScreen.tsx
```

**What it checks:**
- Unnecessary re-renders
- Memoization opportunities
- Component structure optimization
- Data fetching patterns
- List rendering efficiency
- Bundle size concerns
- Event handler optimization
- Suspense and streaming patterns

**Output includes:**
- Performance score
- Critical performance issues
- Optimization opportunities prioritized by impact
- Benchmarking suggestions for React DevTools Profiler
- Code examples showing optimizations

---

### 4. `/accessibilitycheck` - Accessibility Compliance Audit

Audits components for WCAG 2.1 Level AA compliance and a11y best practices.

**Usage:**
```bash
/accessibilitycheck frontend/src/components/RouteCard.tsx
```

**What it checks:**
- Color contrast ratios (4.5:1 for text, 3:1 for UI)
- Keyboard accessibility
- Focus management
- Screen reader compatibility
- ARIA implementation
- Touch target sizing
- Semantic HTML usage

**Output includes:**
- Accessibility score
- WCAG violations by severity
- Testing recommendations (keyboard, screen reader)
- Before/after code examples
- Automated testing tool suggestions

---

## 📖 Guidelines Reference

### UI Guidelines (`guidelines/ux-guidelines/ui.md`)

Comprehensive visual design rules including:
- Gaming-first aesthetic principles
- Color theory and accessibility
- Component design standards (MUI v7)
- Typography system
- Responsive design patterns
- Animation and motion guidelines
- Design anti-patterns to avoid

### UX Guidelines (`guidelines/ux-guidelines/ux.md`)

User experience patterns and flows:
- Primary user objectives and tasks
- Information architecture
- Progressive disclosure strategies
- Interaction patterns
- Accessibility requirements (WCAG 2.1 AA)
- Mobile vs. desktop considerations
- Platform conventions (React 19, MUI v7, TanStack Router)

---

## 💡 Usage Examples

### Example 1: Auditing a New Component

You've just created a new `QuestCard` component and want to ensure it meets all guidelines:

```bash
# First, add the file to context
@frontend/src/components/QuestCard.tsx

# Run comprehensive checks
/uxuiaudit frontend/src/components/QuestCard.tsx
/accessibilitycheck frontend/src/components/QuestCard.tsx
/performanceaudit frontend/src/components/QuestCard.tsx
```

### Example 2: Reviewing Before Pull Request

Before submitting a PR, review your changes:

```bash
# Review the main component
/componentreview frontend/src/screens/NewFeatureScreen.tsx

# Check UI/UX compliance
/uxuiaudit frontend/src/screens/NewFeatureScreen.tsx

# Verify accessibility
/accessibilitycheck frontend/src/screens/NewFeatureScreen.tsx
```

### Example 3: Performance Optimization Sprint

Investigating performance issues in the planner view:

```bash
# Audit the main screen
/performanceaudit frontend/src/screens/PlannerScreen.tsx

# Check child components
/performanceaudit frontend/src/components/MapVisualization.tsx
/performanceaudit frontend/src/components/AreaList.tsx
```

### Example 4: Accessibility Sweep

Ensuring the entire app is accessible:

```bash
# Check key user flows
/accessibilitycheck frontend/src/components/ItemSearch.tsx
/accessibilitycheck frontend/src/components/MapSelector.tsx
/accessibilitycheck frontend/src/components/RouteGenerator.tsx
/accessibilitycheck frontend/src/components/RouteVisualization.tsx
```

---

## 🔧 Customization

### Adding New Commands

Create new `.toml` files in the `.gemini/commands/` directory:

```toml
description = "Your command description"

prompt = """
Your command prompt here.

Reference guidelines: @{guidelines/ux-guidelines/ui.md}
Reference component: @{{component_file}}
"""

[[args]]
name = "component_file"
description = "The file to process"
required = true
```

### Updating Guidelines

Modify the guideline files to reflect evolving project standards:
- `guidelines/ux-guidelines/ui.md` - Visual design rules
- `guidelines/ux-guidelines/ux.md` - User experience patterns

All audit commands automatically use the latest guideline versions.

---

## 🎯 Best Practices

### When to Use Each Command

| Command | When to Use |
|---------|-------------|
| `/uxuiaudit` | New components, UI changes, design reviews |
| `/componentreview` | Code reviews, refactoring, learning |
| `/performanceaudit` | Performance issues, before production, optimization |
| `/accessibilitycheck` | New features, user-facing changes, compliance |

### Workflow Integration

**Development Phase:**
1. Build your component
2. Run `/componentreview` for code quality
3. Run `/uxuiaudit` for design compliance

**Pre-Commit:**
1. Run `/accessibilitycheck` on modified files
2. Run `/performanceaudit` on complex components

**Pull Request:**
1. Include audit results in PR description
2. Address critical issues before requesting review
3. Document any guideline exceptions

---

## 📚 Additional Resources

### Project Documentation
- [CLAUDE.md](../CLAUDE.md) - Project context and development standards
- [Frontend Guidelines](../frontend/README.md) - React/TypeScript conventions

### External References
- [Material UI v7 Documentation](https://mui.com/material-ui/)
- [React 19 Release Notes](https://react.dev/)
- [TanStack Router Docs](https://tanstack.com/router/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

---

## 🤝 Contributing

### Improving Commands

If you find ways to improve the audit commands:
1. Edit the relevant `.toml` file in `.gemini/commands/`
2. Test the changes with sample components
3. Update this README if the command behavior changes
4. Commit changes with a descriptive message

### Updating Guidelines

When updating design/UX guidelines:
1. Modify `guidelines/ux-guidelines/ui.md` or `ux.md`
2. Ensure changes reflect team consensus
3. Run audits on existing components to verify guidelines are reasonable
4. Document any breaking changes from previous guidelines

---

## 🐛 Troubleshooting

### Command Not Found

If `/uxuiaudit` isn't recognized:
1. Ensure you're in the project root directory
2. Verify `.gemini/commands/` exists with `.toml` files
3. Restart Gemini CLI if needed
4. Run `/help` to see available commands

### Guideline Files Not Loading

If audits don't reference guidelines:
1. Check `guidelines/ux-guidelines/ui.md` and `ux.md` exist
2. Verify file paths in `.toml` files are correct
3. Ensure files have proper read permissions

### Performance Issues

If audits are slow:
1. Use specific file paths instead of entire directories
2. Break large components into smaller ones before auditing
3. Consider limiting context in Gemini CLI

---

## 📝 License

These custom commands and guidelines are part of the ARC Raiders Loot Planner project and follow the same license as the main project.

---

**Happy Coding! May your routes be optimal and your loot be legendary.** 🎮✨
