I want agent like you to be prompted for set of instructions to follow.
where should I place those?
I am thinking about

https://agents.md/

as protocol.
but how do I really force (say, Codex) to always follow it?

so my planned prompt was next.
but I think it lacks splitting on correct sections and is way to formal.

====

now, given that we have repo structure, let's build reusable instructions for future you (or your automated colleagues).

follow the approach described in

https://agents.md/

but be simplistic and pragmatic.
no instructions specific for an agent type should be added, think in term of generic agentic entity.
place instructions into this file at the root.

next are the rules to cement in it:
- aforementioned AGENTS.MD is not editable unless explicitly asked
  - each subproject is independent
-- should not be used as dependency by other
-- libraries can be shared, but should be minified to the least needed set

- for each sub-project we have next files:
-- TASKS.MD: read-only, no changes allowed
-- HOWTORUN.MD: editable if changes added to project implies edit, no permission should be asked
-- README.MD: editable if changes added to project implies edit, but permission should be asked