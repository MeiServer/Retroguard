package com.rl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rl.obf.Cl;
import com.rl.obf.ClassTree;
import com.rl.obf.Fd;
import com.rl.obf.Md;
import com.rl.obf.Pk;
import com.rl.obf.TreeItem;
import com.rl.obf.Version;
import com.rl.obf.classfile.ClassConstants;
import com.rl.obf.classfile.ClassFile;
import com.rl.obf.classfile.ClassFileException;

public class NameProvider {
	public static final int CLASSIC_MODE = 0;
	public static final int CHANGE_NOTHING_MODE = 1;
	public static final int DEOBFUSCATION_MODE = 2;
	public static final int REOBFUSCATION_MODE = 3;

	public static final String DEFAULT_CFG_FILE_NAME = "retroguard.cfg";

	public static int uniqueStart = 100000;
	public static int currentMode = NameProvider.CLASSIC_MODE;
	public static boolean quiet = false;
	public static boolean oldHash = false;
	public static boolean repackage = true;
	public static boolean fixShadowed = true;
	public static boolean multipass = true;
	public static boolean verbose = false;
	public static boolean fullMap = false;

	private static Set<File> obfFiles = new HashSet<>();
	private static Set<File> reobFiles = new HashSet<>();
	private static File npLog = null;
	private static File roLog = null;

	private static Set<String> protectedPackages = new HashSet<>();

	private static Map<String, PackageEntry> packagesObf2Deobf = new HashMap<>();
	private static Map<String, PackageEntry> packagesDeobf2Obf = new HashMap<>();
	private static Map<String, ClassEntry> classesObf2Deobf = new HashMap<>();
	private static Map<String, ClassEntry> classesDeobf2Obf = new HashMap<>();
	private static Map<String, MethodEntry> methodsObf2Deobf = new HashMap<>();
	private static Map<String, MethodEntry> methodsDeobf2Obf = new HashMap<>();
	private static Map<String, FieldEntry> fieldsObf2Deobf = new HashMap<>();
	private static Map<String, FieldEntry> fieldsDeobf2Obf = new HashMap<>();

	public static String[] parseCommandLine(final String[] args) throws IOException {
		if (args.length > 0 && (args[0].equalsIgnoreCase("-searge") || args[0].equalsIgnoreCase("-notch"))) {
			return NameProvider.parseNameSheetModeArgs(args);
		}

		if (args.length < 5) {
			return args;
		}

		int idx;
		try {
			idx = Integer.parseInt(args[4]);
		} catch (final NumberFormatException e) {
			throw new NumberFormatException("Invalid start index: " + args[4]);
		}

		NameProvider.uniqueStart = idx;

		final String[] newArgs = new String[4];

		for (int i = 0; i < 4; ++i) {
			newArgs[i] = args[i];
		}

		return newArgs;
	}

	private static String[] parseNameSheetModeArgs(final String[] args) throws IOException {
		if (args.length < 2) {
			return null;
		}

		if (args[0].equalsIgnoreCase("-searge")) {
			NameProvider.currentMode = NameProvider.DEOBFUSCATION_MODE;
		} else if (args[0].equalsIgnoreCase("-notch")) {
			NameProvider.currentMode = NameProvider.REOBFUSCATION_MODE;

			// repackage mode required when reobfing
			NameProvider.repackage = true;
		} else {
			return null;
		}

		final String configFileName = args[1];
		final File configFile = new File(configFileName);
		if (!configFile.exists() || !configFile.isFile()) {
			throw new FileNotFoundException("Could not find config file " + configFileName);
		}

		String reobinput = null;
		String reoboutput = null;
		FileReader fileReader = null;
		BufferedReader reader = null;
		final String[] newArgs = new String[4];
		try {
			fileReader = new FileReader(configFile);
			reader = new BufferedReader(fileReader);
			String line = "";
			while (line != null) {
				line = reader.readLine();
				if (line == null || line.trim().startsWith("#")) {
					continue;
				}

				final String[] defines = line.split("=");
				if (defines.length > 1) {
					defines[1] = line.substring(defines[0].length() + 1).trim();
					defines[0] = defines[0].trim();

					if (defines[0].equalsIgnoreCase("deob")) {
						final File obfFile = new File(defines[1]);
						if (obfFile.isFile()) {
							NameProvider.obfFiles.add(obfFile);
						} else {
							throw new FileNotFoundException("Could not find obf file " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("packages")) {
						final File packagesFile = new File(defines[1]);
						if (packagesFile.isFile()) {
							NameProvider.obfFiles.add(packagesFile);
						} else {
							throw new FileNotFoundException("Could not find packages file " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("classes")) {
						final File classesFile = new File(defines[1]);
						if (classesFile.isFile()) {
							NameProvider.obfFiles.add(classesFile);
						} else {
							throw new FileNotFoundException("Could not find classes file " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("methods")) {
						final File methodsFile = new File(defines[1]);
						if (methodsFile.isFile()) {
							NameProvider.obfFiles.add(methodsFile);
						} else {
							throw new FileNotFoundException("Could not find methods file " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("fields")) {
						final File fieldsFile = new File(defines[1]);
						if (fieldsFile.isFile()) {
							NameProvider.obfFiles.add(fieldsFile);
						} else {
							throw new FileNotFoundException("Could not find fields file " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("reob")) {
						final File reobFile = new File(defines[1]);
						if (reobFile.isFile()) {
							NameProvider.reobFiles.add(reobFile);
						} else {
							// only warn about reob file if we are in reob mode
							if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
								throw new FileNotFoundException("Could not find reob file " + defines[1]);
							}
						}
					} else if (defines[0].equalsIgnoreCase("input")) {
						newArgs[0] = defines[1];
					} else if (defines[0].equalsIgnoreCase("output")) {
						newArgs[1] = defines[1];
					} else if (defines[0].equalsIgnoreCase("reobinput")) {
						reobinput = defines[1];
					} else if (defines[0].equalsIgnoreCase("reoboutput")) {
						reoboutput = defines[1];
					} else if (defines[0].equalsIgnoreCase("script")) {
						newArgs[2] = defines[1];
					} else if (defines[0].equalsIgnoreCase("log")) {
						newArgs[3] = defines[1];
					} else if (defines[0].equalsIgnoreCase("nplog")) {
						NameProvider.npLog = new File(defines[1]);
						if (NameProvider.npLog.exists() && !NameProvider.npLog.isFile()) {
							NameProvider.npLog = null;
						}
					} else if (defines[0].equalsIgnoreCase("rolog")) {
						NameProvider.roLog = new File(defines[1]);
						if (NameProvider.roLog.exists() && !NameProvider.roLog.isFile()) {
							NameProvider.roLog = null;
						}
					} else if (defines[0].equalsIgnoreCase("startindex")) {
						try {
							final int start = Integer.parseInt(defines[1]);
							NameProvider.uniqueStart = start;
						} catch (final NumberFormatException e) {
							throw new NumberFormatException("Invalid start index: " + defines[1]);
						}
					} else if (defines[0].equalsIgnoreCase("protectedpackage")) {
						NameProvider.protectedPackages.add(defines[1]);
					} else if (defines[0].equalsIgnoreCase("quiet")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y")) {
							NameProvider.quiet = true;
						}
					} else if (defines[0].equalsIgnoreCase("oldhash")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y")) {
							NameProvider.oldHash = true;
						}
					} else if (defines[0].equalsIgnoreCase("fixshadowed")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("0") || value.equalsIgnoreCase("f") || value.equalsIgnoreCase("n")) {
							NameProvider.fixShadowed = false;
						}
					} else if (defines[0].equalsIgnoreCase("incremental")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y")) {
							NameProvider.repackage = false;
						}
					} else if (defines[0].equalsIgnoreCase("multipass")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("0") || value.equalsIgnoreCase("f") || value.equalsIgnoreCase("n")) {
							NameProvider.multipass = false;
						}
					} else if (defines[0].equalsIgnoreCase("verbose")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y")) {
							NameProvider.verbose = true;
						}
					} else if (defines[0].equalsIgnoreCase("fullmap")) {
						final String value = defines[1].substring(0, 1);
						if (value.equalsIgnoreCase("1") || value.equalsIgnoreCase("t") || value.equalsIgnoreCase("y")) {
							NameProvider.fullMap = true;
						}
					}
					// Used to change the class identifier, useful when obfuscating to srg names.
					else if (defines[0].equalsIgnoreCase("identifier")) {
						Version.setClassIdString(defines[1].trim());
					}
				}
			}
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (final IOException e) {
				// ignore
			}
		}

		if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			newArgs[0] = reobinput;
			newArgs[1] = reoboutput;
		}

		if (newArgs[0] == null || newArgs[1] == null || newArgs[2] == null || newArgs[3] == null) {
			return null;
		}

		NameProvider.initLogfiles();
		NameProvider.readSRGFiles();

		return newArgs;
	}

	private static void initLogfiles() throws IOException {
		File logFile = null;
		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			logFile = NameProvider.npLog;
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			logFile = NameProvider.roLog;
		}

		if (logFile != null) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(logFile);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (final IOException e) {
						// ignore
					}
				}
			}
		}
	}

	private static void readSRGFiles() throws IOException {
		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			for (final File f : NameProvider.obfFiles) {
				NameProvider.readSRGFile(f);
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			for (final File f : NameProvider.reobFiles) {
				NameProvider.readSRGFile(f);
			}
		}
	}

	private static void readSRGFile(final File f) throws IOException {
		final List<String> lines = NameProvider.readAllLines(f);

		int line_number = 1;
		for (String line : lines) {
			line = line.trim();
			if (line.length() > 0) {
				try {
					if (line.startsWith("PK: ")) {
						NameProvider.addPackageLine(line);
					} else if (line.startsWith("CL: ")) {
						NameProvider.addClassLine(line);
					} else if (line.startsWith("MD: ")) {
						NameProvider.addMethodLine(line);
					} else if (line.startsWith("FD: ")) {
						NameProvider.addFieldLine(line);
					} else if (!line.startsWith("#")) {
						throw new IllegalArgumentException("Invalid line");
					}
				} catch (final IllegalArgumentException e) {
					throw new IllegalArgumentException("in file " + f.getName() + " line " + line_number + "\n\t"
							+ (e.getMessage() != null ? e.getMessage() : "") + "\n\t" + line);
				}
			}

			line_number++;
		}
	}

	private static void addPackageLine(final String line) {
		final String[] lineParts = line.split(" ");
		if (lineParts.length != 3) {
			throw new IllegalArgumentException("Invalid package line");
		}

		final PackageEntry entry = new PackageEntry();
		if (lineParts[1].equals(".")) {
			entry.obfName = "";
		} else {
			entry.obfName = lineParts[1];
		}
		if (lineParts[2].equals(".")) {
			entry.deobfName = "";
		} else {
			entry.deobfName = lineParts[2];
		}

		PackageEntry oldEntry;
		oldEntry = NameProvider.packagesObf2Deobf.put(entry.obfName, entry);
		if (oldEntry != null) {
			throw new IllegalArgumentException("Duplicate deobf package with " + oldEntry);
		}
		oldEntry = NameProvider.packagesDeobf2Obf.put(entry.deobfName, entry);
		if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE && oldEntry != null) {
			throw new IllegalArgumentException("Duplicate reobf package with " + oldEntry);
		}
	}

	private static void addClassLine(final String line) {
		final String[] lineParts = line.split(" ");
		if (lineParts.length != 3) {
			throw new IllegalArgumentException("Invalid class line");
		}

		final ClassEntry entry = new ClassEntry();
		entry.obfName = lineParts[1];
		entry.deobfName = lineParts[2];

		ClassEntry oldEntry;
		oldEntry = NameProvider.classesObf2Deobf.put(entry.obfName, entry);
		if (oldEntry != null) {
			throw new IllegalArgumentException("Duplicate deobf class with " + oldEntry);
		}
		oldEntry = NameProvider.classesDeobf2Obf.put(entry.deobfName, entry);
		if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE && oldEntry != null) {
			throw new IllegalArgumentException("Duplicate reobf class with " + oldEntry);
		}
	}

	private static void addMethodLine(final String line) {
		final String[] lineParts = line.split(" ");
		if (lineParts.length != 5) {
			if (lineParts.length == 4) {
				if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
					throw new IllegalArgumentException("Missing method descriptor for reobf");
				}
			} else {
				throw new IllegalArgumentException("Invalid method line");
			}
		}

		final MethodEntry entry = new MethodEntry();
		entry.obfName = lineParts[1];
		entry.obfDesc = lineParts[2];
		entry.deobfName = lineParts[3];
		if (lineParts.length == 5) {
			entry.deobfDesc = lineParts[4];
		} else {
			entry.deobfDesc = lineParts[2];
		}

		try {
			ClassFile.parseMethodDescriptor(entry.obfDesc);
		} catch (final ClassFileException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		try {
			ClassFile.parseMethodDescriptor(entry.deobfDesc);
		} catch (final ClassFileException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		MethodEntry oldEntry;
		oldEntry = NameProvider.methodsObf2Deobf.put(entry.obfName + entry.obfDesc, entry);
		if (oldEntry != null) {
			throw new IllegalArgumentException("Duplicate deobf method with " + oldEntry);
		}
		oldEntry = NameProvider.methodsDeobf2Obf.put(entry.deobfName + entry.deobfDesc, entry);
		if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE && oldEntry != null) {
			throw new IllegalArgumentException("Duplicate reobf method with " + oldEntry);
		}
	}

	private static void addFieldLine(final String line) {
		final String[] lineParts = line.split(" ");
		if (lineParts.length != 3) {
			throw new IllegalArgumentException("Invalid field line");
		}

		final FieldEntry entry = new FieldEntry();
		entry.obfName = lineParts[1];
		entry.deobfName = lineParts[2];

		FieldEntry oldEntry;
		oldEntry = NameProvider.fieldsObf2Deobf.put(entry.obfName, entry);
		if (oldEntry != null) {
			throw new IllegalArgumentException("Duplicate deobf field with " + oldEntry);
		}
		oldEntry = NameProvider.fieldsDeobf2Obf.put(entry.deobfName, entry);
		if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE && oldEntry != null) {
			throw new IllegalArgumentException("Duplicate reobf field with " + oldEntry);
		}
	}

	private static List<String> readAllLines(final File file) throws IOException {
		final List<String> lines = new ArrayList<>();

		FileReader fileReader = null;
		BufferedReader reader = null;
		try {
			fileReader = new FileReader(file);
			reader = new BufferedReader(fileReader);

			String line = reader.readLine();
			while (line != null) {
				lines.add(line);
				line = reader.readLine();
			}
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (fileReader != null) {
					fileReader.close();
				}
			} catch (final IOException e) {
				// ignore
			}
		}

		return lines;
	}

	public static void log(final String text) {
		NameProvider.log(text, false, false);
	}

	public static void errorLog(final String text) {
		NameProvider.log(text, true, false);
	}

	public static void verboseLog(final String text) {
		NameProvider.log(text, false, true);
	}

	public static void log(final String text, final boolean error, final boolean verbose) {
		if (!NameProvider.verbose && verbose) {
			return;
		}

		if (!NameProvider.quiet && !error) {
			System.out.println(text);
		}

		if (error) {
			System.err.println(text);
		}

		File log = null;
		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			log = NameProvider.npLog;
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			log = NameProvider.roLog;
		}

		if (log == null) {
			return;
		}

		FileWriter fileWriter = null;
		BufferedWriter writer = null;
		try {
			fileWriter = new FileWriter(log, true);
			writer = new BufferedWriter(fileWriter);

			writer.write(text);
			writer.newLine();

			writer.flush();
		} catch (final IOException e) {
			return;
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
				if (fileWriter != null) {
					fileWriter.close();
				}
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	public static void retainFromSRG(final ClassTree classTree) {
		if ((NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE)
				|| (NameProvider.currentMode == NameProvider.CLASSIC_MODE)) {
			return;
		}

		for (final String pkg : NameProvider.protectedPackages) {
			try {
				classTree.retainClass(pkg + "/**", false, true, false, false, false, null, false, 0, 0);
				classTree.retainMethod(pkg + "/**", "*", false, null, false, ClassConstants.ACC_PRIVATE,
						ClassConstants.ACC_PRIVATE);
				classTree.retainField(pkg + "/**", "*", false, null, false, ClassConstants.ACC_PRIVATE,
						ClassConstants.ACC_PRIVATE);
			} catch (final ClassFileException e) {
				// ignore
			}
		}

		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			for (final PackageEntry pkEntry : NameProvider.packagesObf2Deobf.values()) {
				Pk pk = null;
				try {
					pk = classTree.getPk(pkEntry.obfName);
				} catch (final ClassFileException e) {
					// ignore
				}

				if (pk == null) {
					NameProvider.errorLog("# Warning: package " + pkEntry.obfName + " not found in JAR");
				} else {
					pk.setOutput();
				}
			}
			for (final ClassEntry clEntry : NameProvider.classesObf2Deobf.values()) {
				Cl cl = null;
				try {
					cl = classTree.getCl(clEntry.obfName);
				} catch (final ClassFileException e) {
					// ignore
				}

				if (cl == null) {
					NameProvider.errorLog("# Warning: class " + clEntry.obfName + " not found in JAR");
				} else {
					cl.setOutput();
				}
			}
			for (final MethodEntry mdEntry : NameProvider.methodsObf2Deobf.values()) {
				Md md = null;
				try {
					if (NameProvider.multipass) {
						md = (Md) classTree.retainMethodMap(mdEntry.obfName, mdEntry.obfDesc,
								NameProvider.getShortName(mdEntry.deobfName));
					} else {
						md = classTree.getMd(mdEntry.obfName, mdEntry.obfDesc);
					}
				} catch (final ClassFileException e) {
					// ignore
				}

				if (md == null) {
					NameProvider.errorLog("# Warning: method " + mdEntry.obfName + " not found in JAR");
				} else {
					md.setOutput();
				}
			}
			for (final FieldEntry fdEntry : NameProvider.fieldsObf2Deobf.values()) {
				Fd fd = null;
				try {
					if (NameProvider.multipass) {
						fd = (Fd) classTree.retainFieldMap(fdEntry.obfName,
								NameProvider.getShortName(fdEntry.deobfName));
					} else {
						fd = classTree.getFd(fdEntry.obfName);
					}
				} catch (final ClassFileException e) {
					// ignore
				}

				if (fd == null) {
					NameProvider.errorLog("# Warning: field " + fdEntry.obfName + " not found in JAR");
				} else {
					fd.setOutput();
				}
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			for (final PackageEntry pkEntry : NameProvider.packagesDeobf2Obf.values()) {
				Pk pk = null;
				try {
					pk = classTree.getPk(pkEntry.deobfName);
				} catch (final ClassFileException e) {
					// ignore
				}

				if (pk == null) {
					NameProvider.errorLog("# Warning: package " + pkEntry.deobfName + " not found in JAR");
				} else {
					pk.setOutput();
				}
			}
			for (final ClassEntry clEntry : NameProvider.classesDeobf2Obf.values()) {
				Cl cl = null;
				try {
					cl = classTree.getCl(clEntry.deobfName);
				} catch (final ClassFileException e) {
					// ignore
				}

				if (cl == null) {
					NameProvider.errorLog("# Warning: class " + clEntry.deobfName + " not found in JAR");
				} else {
					cl.setOutput();
				}
			}
			for (final MethodEntry mdEntry : NameProvider.methodsDeobf2Obf.values()) {
				Md md = null;
				try {
					if (NameProvider.multipass) {
						md = (Md) classTree.retainMethodMap(mdEntry.deobfName, mdEntry.deobfDesc,
								NameProvider.getShortName(mdEntry.obfName));
					} else {
						md = classTree.getMd(mdEntry.deobfName, mdEntry.deobfDesc);
					}
				} catch (final ClassFileException e) {
					// ignore
				}

				if (md == null) {
					NameProvider.errorLog("# Warning: method " + mdEntry.deobfName + " not found in JAR");
				} else {
					md.setOutput();
				}
			}
			for (final FieldEntry fdEntry : NameProvider.fieldsDeobf2Obf.values()) {
				Fd fd = null;
				try {
					if (NameProvider.multipass) {
						fd = (Fd) classTree.retainFieldMap(fdEntry.deobfName,
								NameProvider.getShortName(fdEntry.obfName));
					} else {
						fd = classTree.getFd(fdEntry.deobfName);
					}
				} catch (final ClassFileException e) {
					// ignore
				}

				if (fd == null) {
					NameProvider.errorLog("# Warning: field " + fdEntry.deobfName + " not found in JAR");
				} else {
					fd.setOutput();
				}
			}
		}
	}

	public static String getNewTreeItemName(final TreeItem ti) {
		if (ti instanceof Pk) {
			return NameProvider.getNewPackageName((Pk) ti);
		} else if (ti instanceof Cl) {
			return NameProvider.getNewClassName((Cl) ti);
		} else if (ti instanceof Md) {
			return NameProvider.getNewMethodName((Md) ti);
		} else if (ti instanceof Fd) {
			return NameProvider.getNewFieldName((Fd) ti);
		} else {
			NameProvider.errorLog("# Warning: trying to rename unknown type " + ti.getFullInName(true));
		}
		return null;
	}

	public static String getNewPackageName(final Pk pk) {
		final String packageName = pk.getInName();
		final String fullPackageName = pk.getFullInName();
		String newFullPackageName = null;
		String newPackageName = null;
		String repackageName = null;

		if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE) {
			pk.setOutput();
			return null;
		}

		if (NameProvider.currentMode == NameProvider.CLASSIC_MODE) {
			newPackageName = "p_" + (++NameProvider.uniqueStart) + "_" + packageName;
			pk.setOutput();
			return newPackageName;
		}

		if (NameProvider.isInProtectedPackage(fullPackageName)) {
			return null;
		}

		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			if (NameProvider.packagesObf2Deobf.containsKey(fullPackageName)) {
				newFullPackageName = NameProvider.packagesObf2Deobf.get(fullPackageName).deobfName;
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			if (NameProvider.packagesDeobf2Obf.containsKey(fullPackageName)) {
				newFullPackageName = NameProvider.packagesDeobf2Obf.get(fullPackageName).obfName;
			}
		}

		if (newFullPackageName != null) {
			// always repackage the default package
			if (fullPackageName.equals("")) {
				newPackageName = newFullPackageName;
				repackageName = newFullPackageName;
			} else {
				newPackageName = NameProvider.getShortName(newFullPackageName);

				if (NameProvider.repackage) {
					repackageName = newFullPackageName;
				}
			}

			if (packageName.equals(repackageName)) {
				repackageName = null;
			}

			if (repackageName != null) {
				pk.setRepackageName(repackageName);
			}
		}

		pk.setOutput();

		return newPackageName;
	}

	public static String getNewClassName(final Cl cl) {
		final String className = cl.getInName();
		final String fullClassName = cl.getFullInName();
		String newFullClassName = null;
		String newClassName = null;
		String newRepackageName = null;

		if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE) {
			cl.setOutput();
			return null;
		}

		if (NameProvider.currentMode == NameProvider.CLASSIC_MODE) {
			// don't rename anonymous inner classes
			if (!cl.isInnerClass() || !Character.isDigit(className.charAt(0))) {
				newClassName = "C_" + (++NameProvider.uniqueStart) + "_" + className;
			}
			cl.setOutput();
			return newClassName;
		}

		if (NameProvider.isInProtectedPackage(fullClassName)) {
			return null;
		}

		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			if (NameProvider.classesObf2Deobf.containsKey(fullClassName)) {
				newFullClassName = NameProvider.classesObf2Deobf.get(fullClassName).deobfName;
			} else {
				if (NameProvider.uniqueStart > 0) {
					// don't rename anonymous inner classes
					if (!cl.isInnerClass() || !Character.isDigit(className.charAt(0))) {
						newClassName = "C_" + NameProvider.uniqueStart++ + "_" + className;
					}
				} else {
					// only warn if we have some other class mappings
					// stops spaming every single class when only repackaging
					if (NameProvider.classesObf2Deobf.size() > 0) {
						NameProvider.errorLog(
								"# Warning: unknown class " + className + " in " + cl.getParent().getFullOutName());
					}
				}
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			if (NameProvider.classesDeobf2Obf.containsKey(fullClassName)) {
				newFullClassName = NameProvider.classesDeobf2Obf.get(fullClassName).obfName;
			}
		}

		if (newFullClassName != null) {
			newClassName = NameProvider.getShortName(newFullClassName);

			if (newClassName != null && newClassName.contains(ClassFile.SEP_INNER)) {
				newClassName = newClassName.substring(newClassName.lastIndexOf(ClassFile.SEP_INNER) + 1);
			}

			if (NameProvider.repackage) {
				newRepackageName = newFullClassName;
			}

			if (fullClassName.equals(newRepackageName)) {
				newRepackageName = null;
			}

			if (newRepackageName != null) {
				cl.setRepackageName(newRepackageName);
			}
		}

		cl.setOutput();

		return newClassName;
	}

	public static String getNewMethodName(final Md md) {
		final String methodName = md.getInName();
		final String methodDescriptor = md.getDescriptor();
		final String fullMethodName = md.getFullInName();
		final String methodNameKey = fullMethodName + methodDescriptor;
		String newFullMethodName = null;
		String newMethodName = null;

		if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE) {
			md.setOutput();
			return null;
		}

		if (NameProvider.currentMode == NameProvider.CLASSIC_MODE) {
			newMethodName = "func_" + (++NameProvider.uniqueStart) + "_" + methodName;
			md.setOutput();
			return newMethodName;
		}

		if (NameProvider.isInProtectedPackage(md.getParent().getFullInName())) {
			return null;
		}

		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			if (NameProvider.methodsObf2Deobf.containsKey(methodNameKey)) {
				newFullMethodName = NameProvider.methodsObf2Deobf.get(methodNameKey).deobfName;
			} else {
				if (NameProvider.uniqueStart > 0) {
					newMethodName = "func_" + NameProvider.uniqueStart++ + "_" + methodName;
				} else {
					// only warn if we know the parent class
					if (md.getParent().isFromScriptMap()) {
						NameProvider.errorLog("# Warning: unknown method " + methodName + " " + methodDescriptor
								+ " in " + md.getParent().getFullOutName());
					}
				}
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			if (NameProvider.methodsDeobf2Obf.containsKey(methodNameKey)) {
				newFullMethodName = NameProvider.methodsDeobf2Obf.get(methodNameKey).obfName;
			}
		}

		if (newFullMethodName != null) {
			newMethodName = NameProvider.getShortName(newFullMethodName);
		}

		md.setOutput();

		return newMethodName;
	}

	public static String getNewFieldName(final Fd fd) {
		final String fieldName = fd.getInName();
		final String fullFieldName = fd.getFullInName();
		String newFullFieldName = null;
		String newFieldName = null;

		if (NameProvider.currentMode == NameProvider.CHANGE_NOTHING_MODE) {
			fd.setOutput();
			return null;
		}

		if (NameProvider.currentMode == NameProvider.CLASSIC_MODE) {
			newFieldName = "field_" + (++NameProvider.uniqueStart) + "_" + fieldName;
			fd.setOutput();
			return newFieldName;
		}

		if (NameProvider.isInProtectedPackage(fd.getParent().getFullInName())) {
			return null;
		}

		if (NameProvider.currentMode == NameProvider.DEOBFUSCATION_MODE) {
			if (NameProvider.fieldsObf2Deobf.containsKey(fullFieldName)) {
				newFullFieldName = NameProvider.fieldsObf2Deobf.get(fullFieldName).deobfName;
			} else {
				if (NameProvider.uniqueStart > 0) {
					newFieldName = "field_" + NameProvider.uniqueStart++ + "_" + fieldName;
				} else {
					// only warn if we know the parent class
					if (fd.getParent().isFromScriptMap()) {
						NameProvider.errorLog(
								"# Warning: unknown field " + fieldName + " in " + fd.getParent().getFullOutName());
					}
				}
			}
		} else if (NameProvider.currentMode == NameProvider.REOBFUSCATION_MODE) {
			if (NameProvider.fieldsDeobf2Obf.containsKey(fullFieldName)) {
				newFullFieldName = NameProvider.fieldsDeobf2Obf.get(fullFieldName).obfName;
			}
		}

		if (newFullFieldName != null) {
			newFieldName = NameProvider.getShortName(newFullFieldName);
		}

		fd.setOutput();

		return newFieldName;
	}

	private static boolean isInProtectedPackage(final String fullInName) {
		for (final String pkg : NameProvider.protectedPackages) {
			if (fullInName.startsWith(pkg + "/")) {
				return true;
			}
		}
		return false;
	}

	private static String getShortName(String name) {
		if (name != null) {
			if (name.contains(ClassFile.SEP_REGULAR)) {
				name = name.substring(name.lastIndexOf(ClassFile.SEP_REGULAR) + 1);
			}
		}

		return name;
	}

	public static void outputPackage(final Pk pk) {
		NameProvider.log("PK: " + pk.getFullInName(true) + " " + pk.getFullOutName(true));
	}

	public static void outputClass(final Cl cl) {
		NameProvider.log("CL: " + cl.getFullInName(true) + " " + cl.getFullOutName(true));
	}

	public static void outputMethod(final Md md) {
		NameProvider.log("MD: " + md.getFullInName(true) + " " + md.getDescriptor() + " " + md.getFullOutName(true)
				+ " " + md.getOutDescriptor());
	}

	public static void outputField(final Fd fd) {
		NameProvider.log("FD: " + fd.getFullInName(true) + " " + fd.getFullOutName(true));
	}
}

class PackageEntry {
	public String obfName;
	public String deobfName;

	@Override
	public String toString() {
		return "PK: " + this.obfName + " " + this.deobfName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.deobfName == null ? 0 : this.deobfName.hashCode());
		result = prime * result + (this.obfName == null ? 0 : this.obfName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final PackageEntry other = (PackageEntry) obj;
		if (this.deobfName == null) {
			if (other.deobfName != null) {
				return false;
			}
		} else if (!this.deobfName.equals(other.deobfName)) {
			return false;
		}
		if (this.obfName == null) {
			if (other.obfName != null) {
				return false;
			}
		} else if (!this.obfName.equals(other.obfName)) {
			return false;
		}
		return true;
	}
}

class ClassEntry {
	public String obfName;
	public String deobfName;

	@Override
	public String toString() {
		return "CL: " + this.obfName + " " + this.deobfName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.deobfName == null ? 0 : this.deobfName.hashCode());
		result = prime * result + (this.obfName == null ? 0 : this.obfName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final ClassEntry other = (ClassEntry) obj;
		if (this.deobfName == null) {
			if (other.deobfName != null) {
				return false;
			}
		} else if (!this.deobfName.equals(other.deobfName)) {
			return false;
		}
		if (this.obfName == null) {
			if (other.obfName != null) {
				return false;
			}
		} else if (!this.obfName.equals(other.obfName)) {
			return false;
		}
		return true;
	}
}

class MethodEntry {
	public String obfName;
	public String obfDesc;
	public String deobfName;
	public String deobfDesc;

	@Override
	public String toString() {
		return "MD: " + this.obfName + " " + this.obfDesc + " " + this.deobfName + " " + this.deobfDesc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.deobfDesc == null ? 0 : this.deobfDesc.hashCode());
		result = prime * result + (this.deobfName == null ? 0 : this.deobfName.hashCode());
		result = prime * result + (this.obfDesc == null ? 0 : this.obfDesc.hashCode());
		result = prime * result + (this.obfName == null ? 0 : this.obfName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final MethodEntry other = (MethodEntry) obj;
		if (this.deobfDesc == null) {
			if (other.deobfDesc != null) {
				return false;
			}
		} else if (!this.deobfDesc.equals(other.deobfDesc)) {
			return false;
		}
		if (this.deobfName == null) {
			if (other.deobfName != null) {
				return false;
			}
		} else if (!this.deobfName.equals(other.deobfName)) {
			return false;
		}
		if (this.obfDesc == null) {
			if (other.obfDesc != null) {
				return false;
			}
		} else if (!this.obfDesc.equals(other.obfDesc)) {
			return false;
		}
		if (this.obfName == null) {
			if (other.obfName != null) {
				return false;
			}
		} else if (!this.obfName.equals(other.obfName)) {
			return false;
		}
		return true;
	}
}

class FieldEntry {
	public String obfName;
	public String deobfName;

	@Override
	public String toString() {
		return "FD: " + this.obfName + " " + this.deobfName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.deobfName == null ? 0 : this.deobfName.hashCode());
		result = prime * result + (this.obfName == null ? 0 : this.obfName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final FieldEntry other = (FieldEntry) obj;
		if (this.deobfName == null) {
			if (other.deobfName != null) {
				return false;
			}
		} else if (!this.deobfName.equals(other.deobfName)) {
			return false;
		}
		if (this.obfName == null) {
			if (other.obfName != null) {
				return false;
			}
		} else if (!this.obfName.equals(other.obfName)) {
			return false;
		}
		return true;
	}
}
