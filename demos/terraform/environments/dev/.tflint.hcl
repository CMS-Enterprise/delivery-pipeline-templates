# Only the bundled terraform ruleset: provider rulesets are plugins tflint
# would download from GitHub at run time.
plugin "terraform" {
  enabled = true
  preset  = "all"
}
