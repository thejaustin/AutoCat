# API Keys Setup Guide

Complete guide to obtaining and configuring LLM provider API keys for AutoCat.

## Overview

AutoCat requires an API key from at least one LLM provider to enable AI-powered categorization. This guide covers:

1. How to obtain API keys from each provider
2. How to configure keys in AutoCat
3. Cost and rate limit information
4. Security best practices

---

## 🔑 Provider Options

### Google AI (Recommended for Free Tier)

**Best for**: Free tier users, fast categorization, generous limits

**Pros**:
- Free tier with generous limits
- Fast response times (Gemini 2.0 Flash)
- Large context window (1M tokens)
- No credit card required for free tier

**Cons**:
- Rate limited (15 requests per minute on free tier)

**Cost**: Free tier available

---

### Claude (Anthropic)

**Best for**: High accuracy, detailed reasoning

**Pros**:
- Excellent categorization accuracy
- Detailed reasoning output
- Fast models (Claude 3.5 Haiku)

**Cons**:
- Requires credit card
- Paid service (no free tier)
- More expensive than Google AI

**Cost**: Pay-as-you-go (starts at $0.25 per million input tokens)

---

### OpenAI

**Best for**: Balanced performance

**Pros**:
- Well-established provider
- Good categorization accuracy
- Fast models (GPT-4o Mini)

**Cons**:
- Requires credit card
- Paid service (no free tier)
- Rate limits on free trial

**Cost**: Pay-as-you-go (starts at $0.15 per million input tokens)

---

### Perplexity

**Best for**: Online search integration

**Pros**:
- Online search capabilities
- Fast models (Llama 3.1 Sonar)
- Good for research-based categorization

**Cons**:
- Requires credit card
- Paid service (limited free tier)
- Less common provider

**Cost**: Free tier with $5 credit, then pay-as-you-go

---

## 🔧 Setup Instructions

### Option 1: Google AI (Gemini)

#### Step 1: Get API Key

1. Go to [Google AI Studio](https://ai.google.dev/)
2. Click "Get API Key" or "Get Started"
3. Sign in with Google account
4. Click "Create API Key"
5. Select or create a Google Cloud project
6. Copy the API key (starts with `AIza...`)

#### Step 2: Configure in AutoCat

**Method A: Settings UI**

1. Open AutoCat
2. Settings → Categorization → LLM Settings
3. Scroll to "Google AI" section
4. Tap "API Key" field
5. Paste your API key
6. Tap "Test Connection" to verify

**Method B: Environment Variable** (More secure)

```bash
# In Termux or shell
export GOOGLE_AI_API_KEY="AIzaSy..."

# AutoCat will automatically detect this
```

#### Step 3: Select Model

1. Settings → Categorization → LLM Settings
2. Under "Google AI", tap "Model"
3. Select "Gemini 2.0 Flash (Recommended)"
4. Save settings

#### Rate Limits

- **Free tier**: 15 requests per minute
- **Quota**: 1,500 requests per day
- **Batch processing**: Recommended to reduce requests

---

### Option 2: Claude (Anthropic)

#### Step 1: Get API Key

1. Go to [Anthropic Console](https://console.anthropic.com/)
2. Sign up or log in
3. Click "API Keys" in sidebar
4. Click "Create Key"
5. Give it a name (e.g., "AutoCat")
6. Copy the key (starts with `sk-ant-...`)

**Note**: Requires credit card on file

#### Step 2: Configure in AutoCat

**Method A: Settings UI**

1. Open AutoCat
2. Settings → Categorization → LLM Settings
3. Tap "LLM Provider" → Select "Claude"
4. Tap "API Key" field under "Claude"
5. Paste your API key
6. Tap "Test Connection" to verify

**Method B: Environment Variable**

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

#### Step 3: Select Model

Recommended models:
- **Claude 3.5 Haiku**: Fast, affordable
- **Claude 3.5 Sonnet**: More accurate, slower

#### Rate Limits

- **Tier 1** (new accounts): 50 requests per minute
- **Tier 2** (after $5 spend): 1,000 requests per minute

---

### Option 3: OpenAI

#### Step 1: Get API Key

1. Go to [OpenAI Platform](https://platform.openai.com/)
2. Sign up or log in
3. Click on your profile → "API Keys"
4. Click "Create new secret key"
5. Name it (e.g., "AutoCat")
6. Copy the key (starts with `sk-...`)

**Note**: Requires credit card on file

#### Step 2: Configure in AutoCat

**Method A: Settings UI**

1. Open AutoCat
2. Settings → Categorization → LLM Settings
3. Tap "LLM Provider" → Select "OpenAI"
4. Tap "API Key" field under "OpenAI"
5. Paste your API key
6. Tap "Test Connection" to verify

**Method B: Environment Variable**

```bash
export OPENAI_API_KEY="sk-..."
```

#### Step 3: Select Model

Recommended model:
- **GPT-4o Mini**: Fast, affordable, good accuracy

#### Rate Limits

- **Free trial**: 3 requests per minute
- **Tier 1** (after $5 spend): 500 requests per minute

---

### Option 4: Perplexity

#### Step 1: Get API Key

1. Go to [Perplexity AI](https://www.perplexity.ai/)
2. Sign up or log in
3. Navigate to API settings
4. Create new API key
5. Copy the key

#### Step 2: Configure in AutoCat

**Method A: Settings UI**

1. Open AutoCat
2. Settings → Categorization → LLM Settings
3. Tap "LLM Provider" → Select "Perplexity"
4. Tap "API Key" field under "Perplexity"
5. Paste your API key
6. Tap "Test Connection" to verify

**Method B: Environment Variable**

```bash
export PERPLEXITY_API_KEY="pplx-..."
```

#### Step 3: Select Model

Recommended model:
- **Llama 3.1 Sonar Small**: Fast, affordable

---

## 💰 Cost Comparison

### For 100 Apps Categorization (with batch processing)

| Provider | Model | Estimated Cost | Free Tier |
|----------|-------|----------------|-----------|
| Google AI | Gemini 2.0 Flash | **$0.00** | ✅ Free |
| Claude | Claude 3.5 Haiku | ~$0.01-0.02 | ❌ Paid |
| OpenAI | GPT-4o Mini | ~$0.01 | ❌ Paid |
| Perplexity | Llama 3.1 Sonar | ~$0.01 | ⚠️ $5 credit |

**Note**: Costs are estimates based on typical app metadata. Actual costs may vary.

### Monthly Cost Estimates

Assuming daily re-categorization of 100 apps:

| Provider | Daily | Monthly (30 days) |
|----------|-------|-------------------|
| Google AI | Free | Free |
| Claude | $0.01 | $0.30 |
| OpenAI | $0.01 | $0.30 |
| Perplexity | Free (with credit) | $0.30 (after credit) |

---

## 🔒 Security Best Practices

### Use Environment Variables

**Why?**
- API keys not stored in app settings
- More secure (not in backups)
- Easy to rotate keys

**How?**

**Termux**:
```bash
# Add to ~/.bashrc or ~/.zshrc
export GOOGLE_AI_API_KEY="your-key-here"
export ANTHROPIC_API_KEY="your-key-here"
export OPENAI_API_KEY="your-key-here"
export PERPLEXITY_API_KEY="your-key-here"

# Reload shell
source ~/.bashrc
```

**Android (via Termux)**:
```bash
# Create a script to set env vars
cat > ~/set-api-keys.sh <<EOF
export GOOGLE_AI_API_KEY="your-key-here"
export ANTHROPIC_API_KEY="your-key-here"
export OPENAI_API_KEY="your-key-here"
export PERPLEXITY_API_KEY="your-key-here"
EOF

# Source before running AutoCat
source ~/set-api-keys.sh
```

### Key Rotation

**Best practice**: Rotate API keys every 90 days

1. Generate new key in provider console
2. Update AutoCat settings or env variable
3. Test connection
4. Delete old key in provider console

### Monitor Usage

**Prevent unexpected charges**:

1. Set up billing alerts in provider console
2. Monitor API usage regularly
3. Set spending limits if available
4. Review AutoCat accuracy metrics (Settings → LLM Settings)

---

## 🧪 Testing Your Configuration

### Test Connection Button

After adding API key:

1. Settings → Categorization → LLM Settings
2. Find your provider section
3. Tap "Test Connection"
4. Wait for result:
   - ✅ **Success**: Shows latency and model version
   - ❌ **Failure**: Shows specific error message

### Common Test Errors

**"Invalid API key"**
- Double-check key is copied correctly
- Verify key is active in provider console
- Check for extra spaces

**"Rate limit exceeded"**
- Wait a few seconds and try again
- Check your provider's rate limits
- Consider upgrading tier

**"Network error"**
- Check internet connection
- Verify provider API is not down
- Check firewall/VPN settings

---

## 📊 Choosing the Right Provider

### Decision Matrix

| Use Case | Recommended Provider | Why |
|----------|---------------------|-----|
| **Just trying AutoCat** | Google AI | Free, no credit card needed |
| **Daily use (free)** | Google AI | Generous free tier |
| **Best accuracy** | Claude 3.5 Haiku | Higher quality categorization |
| **Balanced (cost/quality)** | OpenAI GPT-4o Mini | Good quality, reasonable cost |
| **Auto-select best** | Enable "Auto-Select" | Let AutoCat choose based on accuracy |

### Auto-Select Best Model

**What it does**: AutoCat tracks accuracy for each provider and automatically selects the best performing one.

**How to enable**:
1. Settings → Categorization → LLM Settings
2. Enable "Auto-Select Best Model"
3. Ensure you have API keys for multiple providers
4. AutoCat will analyze 30 days of accuracy data

**Requirements**:
- At least 2 providers configured
- Minimum 10 categorizations per provider
- At least 70% accuracy threshold

---

## 🔄 Switching Providers

### Mid-Use Provider Change

You can switch providers at any time:

1. Settings → Categorization → LLM Settings
2. Tap "LLM Provider"
3. Select new provider
4. Ensure new provider has valid API key
5. Test connection

**Note**: Existing categorizations remain unchanged. Only new categorizations use new provider.

### Re-categorize with New Provider

To re-categorize all apps with new provider:

1. Switch provider (above steps)
2. Settings → Categorization
3. Tap "Re-categorize All Apps"
4. Wait for completion

---

## ❓ FAQ

**Q: Can I use multiple providers?**
A: Yes! Add keys for multiple providers and enable "Auto-Select Best Model" to let AutoCat choose.

**Q: Do I need to pay for AutoCat?**
A: No, AutoCat is free. You only pay for LLM provider API usage (Google AI has free tier).

**Q: How many apps can I categorize for free?**
A: With Google AI free tier: ~1,500 categorizations per day (more with batch processing).

**Q: What if my key stops working?**
A: Check provider console for:
- Key status (active/revoked)
- Billing issues
- Rate limit exceeded
- API quota exhausted

**Q: Can I share my API key?**
A: No, API keys are personal. Sharing violates provider terms of service.

**Q: How do I delete my API key from AutoCat?**
A: Settings → Categorization → LLM Settings → Tap API key field → Delete text

---

## 🆘 Troubleshooting

### "Connection failed" Error

1. **Check internet connection**
2. **Verify API key is correct** (copy/paste again)
3. **Test in provider console** (make test API call)
4. **Check provider status** (is API down?)

### "Quota exceeded" Error

1. **Check provider dashboard** for usage limits
2. **Wait for quota reset** (usually daily/hourly)
3. **Upgrade tier** if available
4. **Switch to different provider** temporarily

### "Invalid model" Error

1. **Update AutoCat** to latest version
2. **Check model availability** in provider docs
3. **Select different model** in settings
4. **Report issue** if model should be available

---

## 📚 Additional Resources

### Provider Documentation

- [Google AI Studio Docs](https://ai.google.dev/docs)
- [Anthropic API Docs](https://docs.anthropic.com/)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [Perplexity API Docs](https://docs.perplexity.ai/)

### AutoCat Documentation

- [User Guide](User-Guide.md) - Using AutoCat features
- [Developer Guide](Developer-Guide.md) - Contributing to AutoCat
- [Troubleshooting](User-Guide.md#troubleshooting) - Common issues

---

*Last Updated: 2025-12-25*
