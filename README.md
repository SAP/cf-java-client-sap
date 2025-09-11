# Project Status: Archived

⚠️ **This project has been archived** ⚠️

The **cf-java-client-sap** repository is no longer actively maintained.  
It was previously a facade/wrapper around the [cf-java-client](https://github.com/cloudfoundry/cf-java-client).  
The code has now been merged into another [project](https://github.com/cloudfoundry/multiapps-controller) for internal use only.

---

## Guidance for Users

If you are currently using **cf-java-client-sap**,  
➡️ the recommended path forward is to **use the official [cf-java-client](https://github.com/cloudfoundry/cf-java-client) directly**.

This ensures you are always up to date with the latest features, fixes, and improvements from the Cloud Foundry community.

---

## Why was this change made?

- Maintaining a custom facade added unnecessary complexity.  
- The functionality has been consolidated into a project that depends on it internally.  
- For external consumers, using the pure **cf-java-client** is simpler and future-proof.  

---

🙏 We thank all contributors and users of **cf-java-client-sap** for their support!
