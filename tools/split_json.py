#!/usr/bin/env python3
"""
JSON Splitter Utility for AI Analysis Datasets

This utility splits a large dataset JSON file (such as radion-analysis.json) into smaller,
manageable part files of approximately 50-100 MB each (default targeting ~70 MB per part).

Key Architectural Guarantees & Features:
- Zero Data Regeneration / Modification: Operates via line-level boundary detection without
  passing email objects through a JSON decoder or re-serializer. Every single byte, space, and
  character inside the email objects is preserved 100% identically from the source file.
- Exact Schema Preservation: Each output part file is a valid, self-contained JSON document
  retaining the exact root schema (version, generatedAt, totalEmails for that part, emails array).
- Atomic Chunking: Guaranteed never to split an email object across file boundaries.
- Rigorous Validation: Every generated part file is structurally parsed with `json.load()` before
  being finalized to guarantee 100% UTF-8 JSON validity.
- Manifest Generation: Produces a comprehensive `manifest.json` documenting total files, total
  emails, generation timestamps, file sizes, and email counts per part.
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timezone
from typing import Any, Dict, List, Tuple


DEFAULT_INPUT_FILE = "radion-analysis.json"
DEFAULT_PREFIX = "radion-analysis"
DEFAULT_TARGET_MB = 70.0  # Safe target within the 50-100 MB requirement


def parse_root_metadata(input_path: str) -> Tuple[int, str, int]:
    """
    Reads the top lines of the input JSON to extract version, generatedAt, and totalEmails
    without loading the entire multi-hundred megabyte file into memory.
    """
    version = 1
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    total_emails = -1

    with open(input_path, "r", encoding="utf-8") as f:
        for idx in range(20):
            line = f.readline().strip()
            if not line:
                break
            if line.startswith('"version":'):
                try:
                    val = line.split(":", 1)[1].strip().rstrip(",")
                    version = int(val)
                except Exception:
                    pass
            elif line.startswith('"generatedAt":'):
                try:
                    val = line.split(":", 1)[1].strip().rstrip(",").strip('"')
                    if val:
                        generated_at = val
                except Exception:
                    pass
            elif line.startswith('"totalEmails":'):
                try:
                    val = line.split(":", 1)[1].strip().rstrip(",")
                    total_emails = int(val)
                except Exception:
                    pass
            elif line.startswith('"emails":'):
                break

    return version, generated_at, total_emails


def write_part_file(
    output_path: str,
    version: int,
    generated_at: str,
    chunks: List[str]
) -> int:
    """
    Writes a list of raw email JSON string chunks to a part file, ensuring proper
    JSON array formatting and structural validation before atomic rename.
    Returns the final file size in bytes.
    """
    tmp_path = f"{output_path}.tmp"
    part_count = len(chunks)

    out_dir = os.path.dirname(os.path.abspath(output_path))
    if out_dir and not os.path.exists(out_dir):
        os.makedirs(out_dir, exist_ok=True)

    with open(tmp_path, "w", encoding="utf-8") as out_f:
        out_f.write("{\n")
        out_f.write(f'  "version": {version},\n')
        out_f.write(f'  "generatedAt": "{generated_at}",\n')
        out_f.write(f'  "totalEmails": {part_count},\n')
        out_f.write('  "emails": [\n')

        for idx, chunk in enumerate(chunks, 1):
            # Clean trailing comma or whitespace from the collected chunk
            chunk_clean = chunk.rstrip("\r\n, ")
            if idx < part_count:
                out_f.write(chunk_clean + ",\n")
            else:
                out_f.write(chunk_clean + "\n")

        out_f.write("  ]\n")
        out_f.write("}\n")

    # Structural syntax validation
    try:
        with open(tmp_path, "r", encoding="utf-8") as val_f:
            data = json.load(val_f)
            val_count = len(data.get("emails", []))
            if val_count != part_count:
                raise ValueError(f"Validation mismatch: expected {part_count} emails, found {val_count}")
    except Exception as e:
        print(f"[ERROR] JSON validation failed for part file {output_path}: {e}", file=sys.stderr)
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
        sys.exit(1)

    if os.path.exists(output_path):
        os.remove(output_path)
    os.rename(tmp_path, output_path)

    return os.path.getsize(output_path)


def split_json_dataset(input_path: str, output_dir: str, prefix: str, target_mb: float) -> None:
    t0 = time.time()
    print(f"[INFO] Starting JSON Dataset Splitter")
    print(f"[INFO] Input File  : {os.path.abspath(input_path)}")
    print(f"[INFO] Output Dir  : {os.path.abspath(output_dir)}")
    print(f"[INFO] Target Size : ~{target_mb:.1f} MB per part\n")

    if not os.path.exists(input_path):
        print(f"[ERROR] Input file does not exist: {input_path}", file=sys.stderr)
        sys.exit(1)

    version, generated_at, orig_total_emails = parse_root_metadata(input_path)
    print(f"[INFO] Root Metadata: version={version}, generatedAt='{generated_at}', totalEmails={orig_total_emails:,d}")

    target_bytes = int(target_mb * 1024 * 1024)
    
    part_num = 1
    current_chunks: List[str] = []
    current_bytes = 0
    total_emails_processed = 0
    manifest_files: List[Dict[str, Any]] = []

    print("[INFO] Scanning objects and streaming part files...")
    t_scan_start = time.time()

    with open(input_path, "r", encoding="utf-8") as f:
        in_object = False
        object_lines: List[str] = []

        for line in f:
            if line.startswith("    {") and line.strip() == "{":
                in_object = True
                object_lines = [line]
            elif in_object:
                object_lines.append(line)
                stripped = line.strip()
                if stripped in ("},", "}") and line.startswith("    }"):
                    in_object = False
                    chunk_str = "".join(object_lines)
                    chunk_len = len(chunk_str.encode("utf-8"))

                    current_chunks.append(chunk_str)
                    current_bytes += chunk_len
                    total_emails_processed += 1

                    # If threshold reached, flush current part file
                    if current_bytes >= target_bytes:
                        part_filename = f"{prefix}-part-{part_num:03d}.json"
                        part_path = os.path.join(output_dir, part_filename)
                        
                        size_written = write_part_file(part_path, version, generated_at, current_chunks)
                        size_mb = size_written / (1024 * 1024)
                        print(f"  -> Generated {part_filename:<32} : {len(current_chunks):>5,d} emails ({size_mb:>5.2f} MB)")

                        manifest_files.append({
                            "filename": part_filename,
                            "emailCount": len(current_chunks),
                            "sizeBytes": size_written
                        })

                        part_num += 1
                        current_chunks = []
                        current_bytes = 0

    # Flush remaining chunks into final part file
    if current_chunks:
        part_filename = f"{prefix}-part-{part_num:03d}.json"
        part_path = os.path.join(output_dir, part_filename)
        
        size_written = write_part_file(part_path, version, generated_at, current_chunks)
        size_mb = size_written / (1024 * 1024)
        print(f"  -> Generated {part_filename:<32} : {len(current_chunks):>5,d} emails ({size_mb:>5.2f} MB)")

        manifest_files.append({
            "filename": part_filename,
            "emailCount": len(current_chunks),
            "sizeBytes": size_written
        })

    # Verify total email counts match exactly
    manifest_total_emails = sum(item["emailCount"] for item in manifest_files)
    print(f"\n[INFO] Split complete: processed {manifest_total_emails:,d} emails across {len(manifest_files)} files.")

    if orig_total_emails != -1 and manifest_total_emails != orig_total_emails:
        print(f"[WARNING] Email count mismatch! Original header said {orig_total_emails:,d}, but split found {manifest_total_emails:,d}.", file=sys.stderr)

    # Write manifest.json
    manifest_data = {
        "totalFiles": len(manifest_files),
        "totalEmails": manifest_total_emails,
        "generatedAt": generated_at,
        "files": manifest_files
    }
    manifest_path = os.path.join(output_dir, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as mf:
        json.dump(manifest_data, mf, indent=2, ensure_ascii=False)
        mf.write("\n")

    total_time = time.time() - t0
    print(f"[SUCCESS] Successfully generated manifest at '{manifest_path}'.")
    print(f"[SUCCESS] Total execution time: {total_time:.2f} seconds.")


def main():
    parser = argparse.ArgumentParser(
        description="Split radion-analysis.json into ~50-100 MB part files without modifying data."
    )
    parser.add_argument(
        "-i", "--input-file",
        default=DEFAULT_INPUT_FILE,
        help=f"Input JSON dataset file path (default: {DEFAULT_INPUT_FILE})"
    )
    parser.add_argument(
        "-o", "--output-dir",
        default=".",
        help="Directory to save generated part files and manifest.json (default: current directory)"
    )
    parser.add_argument(
        "-p", "--prefix",
        default=DEFAULT_PREFIX,
        help=f"Filename prefix for output part files (default: {DEFAULT_PREFIX})"
    )
    parser.add_argument(
        "--target-mb",
        type=float,
        default=DEFAULT_TARGET_MB,
        help=f"Target file size in MB per part (default: {DEFAULT_TARGET_MB})"
    )

    args = parser.parse_args()
    split_json_dataset(args.input_file, args.output_dir, args.prefix, args.target_mb)


if __name__ == "__main__":
    main()
