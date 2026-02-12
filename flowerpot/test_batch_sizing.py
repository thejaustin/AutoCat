class ModelInfo:
    def __init__(self, name, context_window):
        self.name = name
        self.context_window = context_window

# Constants from Kotlin file
TOKENS_PER_APP = 50
PROMPT_OVERHEAD = 200
SAFETY_MARGIN = 0.3

class BatchCalculator:
    
    @staticmethod
    def calculate_optimal_batch_size(model_info, total_apps, categories, rate_limit_delay_ms=4000):
        context_window = model_info.context_window
        
        # Estimate category overhead (roughly 4 chars per token)
        category_lines = [f"- {c}" for c in categories]
        category_text = "".join(category_lines) # simplified join
        category_tokens = len(category_text) / 4
        
        available_prompt_tokens = int(context_window * (1 - SAFETY_MARGIN))
        tokens_for_apps = available_prompt_tokens - PROMPT_OVERHEAD - category_tokens
        
        max_apps_per_batch = int(tokens_for_apps / TOKENS_PER_APP)
        if max_apps_per_batch < 1:
            max_apps_per_batch = 1
            
        # Apply limits
        if context_window >= 100000:
            batch_size = max(20, min(max_apps_per_batch, 50))
        elif context_window >= 30000:
            batch_size = max(15, min(max_apps_per_batch, 30))
        else:
            batch_size = max(10, min(max_apps_per_batch, 20))
            
        batches = (total_apps + batch_size - 1) // batch_size
        estimated_tokens = PROMPT_OVERHEAD + category_tokens + (batch_size * TOKENS_PER_APP)
        estimated_time = batches * rate_limit_delay_ms
        
        return {
            "batch_size": batch_size,
            "estimated_tokens": estimated_tokens,
            "estimated_batches": batches,
            "estimated_time_ms": estimated_time
        }

    @staticmethod
    def estimate_time_savings(batch_config, total_apps, rate_limit_delay_ms=4000):
        sequential_time = total_apps * rate_limit_delay_ms
        batch_time = batch_config["estimated_time_ms"]
        
        speedup = sequential_time / batch_time if batch_time > 0 else 0
        
        return {
            "sequential_time_ms": sequential_time,
            "batch_time_ms": batch_time,
            "savings_ms": sequential_time - batch_time,
            "speedup_factor": speedup
        }

def run_tests():
    print("=== Batch Calculator Verification ===\n")
    
    # Setup
    categories = ["Games", "Social", "Productivity", "Tools", "Entertainment"] * 5 # 25 categories
    total_apps = 100
    
    models = [
        ModelInfo("Gemini 2.0 Flash (1M)", 1_000_000),
        ModelInfo("GPT-4o Mini (128K)", 128_000),
        ModelInfo("Claude 3.5 Haiku (200K)", 200_000),
        ModelInfo("Legacy Model (16K)", 16_000)
    ]
    
    for model in models:
        print(f"Testing Model: {model.name}")
        config = BatchCalculator.calculate_optimal_batch_size(model, total_apps, categories)
        savings = BatchCalculator.estimate_time_savings(config, total_apps)
        
        print(f"  Batch Size: {config['batch_size']}")
        print(f"  Batches Required: {config['estimated_batches']}")
        print(f"  Sequential Time: {savings['sequential_time_ms']/1000:.1f}s")
        print(f"  Batch Time:      {savings['batch_time_ms']/1000:.1f}s")
        print(f"  Speedup:         {savings['speedup_factor']:.1f}x")
        print("-" * 40)

if __name__ == "__main__":
    run_tests()
