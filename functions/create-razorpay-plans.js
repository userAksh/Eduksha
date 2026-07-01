/**
 * One-time script to create Razorpay subscription plans with correct paise amounts.
 * Run from the functions/ directory:
 *
 *   node create-razorpay-plans.js <KEY_ID> <KEY_SECRET>
 *
 * Copy the printed plan IDs into the PLAN_* constants in index.js, then redeploy.
 */

const Razorpay = require("razorpay");

const [, , keyId, keySecret] = process.argv;
if (!keyId || !keySecret) {
  console.error("Usage: node create-razorpay-plans.js <KEY_ID> <KEY_SECRET>");
  process.exit(1);
}

const rzp = new Razorpay({ key_id: keyId, key_secret: keySecret });

const plans = [
  {
    slug: "PLAN_TRIAL",
    payload: {
      period: "monthly",
      interval: 1,
      item: { name: "Eduksha Trial", amount: 100, currency: "INR", description: "₹1 trial" },
    },
  },
  {
    slug: "PLAN_MONTHLY",
    payload: {
      period: "monthly",
      interval: 1,
      item: { name: "Eduksha Monthly", amount: 14900, currency: "INR", description: "₹149/month" },
    },
  },
  {
    slug: "PLAN_QUARTERLY",
    payload: {
      period: "monthly",
      interval: 3,
      item: { name: "Eduksha Quarterly", amount: 24900, currency: "INR", description: "₹249 for 3 months" },
    },
  },
  {
    slug: "PLAN_HALFYEARLY",
    payload: {
      period: "monthly",
      interval: 6,
      item: { name: "Eduksha Half-Yearly", amount: 44900, currency: "INR", description: "₹449 for 6 months" },
    },
  },
];

(async () => {
  console.log("Creating Razorpay plans...\n");
  const results = {};
  for (const { slug, payload } of plans) {
    try {
      const plan = await rzp.plans.create(payload);
      results[slug] = plan.id;
      console.log(`✅ ${slug} = "${plan.id}"  (₹${payload.item.amount / 100} every ${payload.interval} month(s))`);
    } catch (err) {
      console.error(`❌ ${slug} failed:`, err?.error?.description ?? err.message);
    }
  }

  console.log("\n── Paste these into functions/index.js ──────────────────────────────────");
  for (const [slug, id] of Object.entries(results)) {
    console.log(`const ${slug.padEnd(16)} = "${id}";`);
  }
})();
